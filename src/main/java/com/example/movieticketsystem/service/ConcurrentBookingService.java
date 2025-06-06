package com.example.movieticketsystem.service;

import com.example.movieticketsystem.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpSession;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Map;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcurrentBookingService {
    private final BookingService bookingService;
    private final int processorCount = Runtime.getRuntime().availableProcessors();
    // Thread pool để xử lý các yêu cầu đặt vé đồng thời
    private final ExecutorService bookingExecutor = Executors.newFixedThreadPool(processorCount * 2);
    
    // Cache để lưu trữ trạng thái ghế, sử dụng ConcurrentHashMap để thread-safe
    private final Map<String, ReentrantLock> seatLocks = new ConcurrentHashMap<>();
    
    // Semaphore để giới hạn số lượng đặt vé đồng thời
    private final Semaphore bookingLimiter = new Semaphore(processorCount * 4);

    /**
     * Đặt nhiều ghế đồng thời cho một screening
     * @return CompletableFuture chứa kết quả đặt vé
     */
    public CompletableFuture<BookingResult> reserveSeatsAsync(Long screeningId, List<Long> seatIds, User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Cố gắng lấy permit từ semaphore
                if (!bookingLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
                    return new BookingResult(false, "Hệ thống đang quá tải. Vui lòng thử lại sau.");
                }

                List<Long> successfulReservations = new CopyOnWriteArrayList<>();
                boolean needsRollback = false;

                try {
                    // Đặt tất cả các ghế
                    for (Long seatId : seatIds) {
                        String lockKey = screeningId + "-" + seatId;
                        ReentrantLock lock = seatLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
                        
                        if (lock.tryLock(5, TimeUnit.SECONDS)) {
                            try {
                                if (bookingService.reserveSeat(screeningId, seatId)) {
                                    successfulReservations.add(seatId);
                                } else {
                                    needsRollback = true;
                                    break;
                                }
                            } finally {
                                lock.unlock();
                            }
                        } else {
                            needsRollback = true;
                            break;
                        }
                    }

                    if (needsRollback || successfulReservations.size() != seatIds.size()) {
                        rollbackReservations(screeningId, successfulReservations);
                        return new BookingResult(false, "Not all seats available. Please try again.");
                    }

                    // Chỉ trả về kết quả đặt chỗ thành công, chưa tạo vé
                    return new BookingResult(true, "Đặt chỗ thành công", null);

                } catch (Exception e) {
                    if (!successfulReservations.isEmpty()) {
                        rollbackReservations(screeningId, successfulReservations);
                    }
                    throw e;
                } finally {
                    bookingLimiter.release();
                }
            } catch (Exception e) {
                log.error("Error in concurrent booking: {}", e.getMessage());
                return new BookingResult(false, "Lỗi hệ thống: " + e.getMessage());
            }
        }, bookingExecutor);
    }

    /**
     * Tạo vé sau khi thanh toán thành công
     */
    public CompletableFuture<BookingResult> createTicketsAfterPayment(Long screeningId, List<Long> seatIds, User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Ticket> tickets = new CopyOnWriteArrayList<>();
                boolean needsRollback = false;

                // Tạo vé cho các ghế đã đặt
                for (Long seatId : seatIds) {
                    Optional<Ticket> ticket = bookingService.createTicket(user, screeningId, seatId);
                    if (ticket.isEmpty()) {
                        needsRollback = true;
                        break;
                    }
                    tickets.add(ticket.get());
                }

                if (needsRollback || tickets.size() != seatIds.size()) {
                    // Nếu tạo vé thất bại, hủy các vé đã tạo và giải phóng ghế
                    for (Ticket ticket : tickets) {
                        bookingService.cancelTicket(ticket.getId(), user.getId());
                    }
                    rollbackReservations(screeningId, seatIds);
                    return new BookingResult(false, "Không thể tạo vé. Vui lòng thử lại.");
                }

                return new BookingResult(true, "Đặt vé thành công", tickets);

            } catch (Exception e) {
                log.error("Error creating tickets: {}", e.getMessage());
                return new BookingResult(false, "Lỗi hệ thống: " + e.getMessage());
            }
        }, bookingExecutor);
    }

    /**
     * Hủy đặt chỗ cho các ghế đã đặt trong trường hợp lỗi
     */
    private void rollbackReservations(Long screeningId, List<Long> seatIds) {
        for (Long seatId : seatIds) {
            String lockKey = screeningId + "-" + seatId;
            ReentrantLock lock = seatLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
            
            try {
                if (lock.tryLock(5, TimeUnit.SECONDS)) {
                    try {
                        bookingService.releaseSeat(screeningId, seatId);
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Error rolling back reservation for seat {}: {}", seatId, e.getMessage());
            } catch (Exception e) {
                log.error("Error rolling back reservation for seat {}: {}", seatId, e.getMessage());
            }
        }
    }

    /**
     * Đóng thread pool khi ứng dụng shutdown
     */
    public void shutdown() {
        bookingExecutor.shutdown();
        try {
            if (!bookingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                bookingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            bookingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 