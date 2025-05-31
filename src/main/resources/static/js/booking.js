document.addEventListener('DOMContentLoaded', function() {
    // Handle seat selection
    const seatElements = document.querySelectorAll('.seat.available');
    const selectedSeatInput = document.getElementById('seatId');
    const selectedSeatDisplay = document.getElementById('selectedSeatDisplay');
    const bookForm = document.getElementById('bookingForm');

    if (seatElements.length > 0) {
        seatElements.forEach(seat => {
            seat.addEventListener('click', function() {
                // Remove selection from all seats
                document.querySelectorAll('.seat.selected').forEach(selected => {
                    selected.classList.remove('selected');
                });

                // Add selection to clicked seat
                this.classList.add('selected');

                // Update hidden input value
                const seatId = this.getAttribute('data-seat-id');
                selectedSeatInput.value = seatId;

                // Update display
                selectedSeatDisplay.textContent = this.getAttribute('data-seat-name');

                // Enable submit button
                const submitButton = bookForm.querySelector('button[type="submit"]');
                if (submitButton) {
                    submitButton.disabled = false;
                }
            });
        });
    }

    // Form submission validation
    if (bookForm) {
        bookForm.addEventListener('submit', function(event) {
            if (!selectedSeatInput.value) {
                event.preventDefault();
                alert('Please select a seat before proceeding.');
            }
        });
    }
});

// Seat auto-selection from URL parameter (if any)
function autoSelectSeat() {
    const urlParams = new URLSearchParams(window.location.search);
    const seatId = urlParams.get('seatId');

    if (seatId) {
        const seatElement = document.querySelector(`.seat[data-seat-id="${seatId}"]`);
        if (seatElement && seatElement.classList.contains('available')) {
            seatElement.click();
        }
    }
}

// Call auto-select function on page load
window.addEventListener('load', autoSelectSeat);