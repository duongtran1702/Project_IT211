package atmin.repository;

import atmin.entity.Booking;
import atmin.entity.Court;
import atmin.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b FROM Booking b WHERE b.user = :user AND b.isDeleted = false")
    Page<Booking> findByUserActive(@Param("user") User user, Pageable pageable);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.court = :court " +
           "AND b.bookingDate = :bookingDate " +
           "AND b.timeSlot = :timeSlot " +
           "AND b.status <> 'CANCELLED' " +
           "AND b.isDeleted = false")
    boolean existsActiveBooking(@Param("court") Court court,
                                @Param("bookingDate") LocalDate bookingDate,
                                @Param("timeSlot") String timeSlot);
}
