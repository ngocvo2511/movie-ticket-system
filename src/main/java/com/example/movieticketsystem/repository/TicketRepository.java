package com.example.movieticketsystem.repository;

import com.example.movieticketsystem.model.Ticket;
import com.example.movieticketsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUserOrderByPurchaseTimeDesc(User user);

    List<Ticket> findByUserAndStatus(User user, Ticket.TicketStatus status);

    List<Ticket> findByScreeningId(Long screeningId);

    // Tìm vé theo userId và trạng thái, sắp xếp theo thời gian mua giảm dần (mới nhất trước)
    List<Ticket> findByUserIdAndStatusOrderByPurchaseTimeDesc(Long userId, Ticket.TicketStatus status);

    // Tìm tất cả vé của một người dùng, sắp xếp theo thời gian mua giảm dần
    List<Ticket> findByUserIdOrderByPurchaseTimeDesc(Long userId);

    // Tìm vé cho các suất chiếu chưa bắt đầu
    List<Ticket> findByUserIdAndScreeningStartTimeAfterOrderByScreeningStartTime(Long userId, LocalDateTime now);

    boolean existsByScreeningId(Long screeningId);

    // Kiểm tra xem có vé active cho screening không
    boolean existsByScreeningIdAndStatusNot(Long screeningId, Ticket.TicketStatus status);
}