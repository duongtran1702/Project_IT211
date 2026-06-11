package atmin.repository;

import atmin.controller.booking.dto.response.BookingResponse;
import atmin.entity.Booking;
import atmin.entity.Court;
import atmin.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT new atmin.controller.booking.dto.response.BookingResponse(" +
           "b.id, b.bookingDate, b.timeSlot, b.totalPrice, b.status, " +
           "c.courtName, cl.name, u.fullName) " +
           "FROM Booking b " +
           "JOIN b.court c " +
           "JOIN c.cluster cl " +
           "JOIN b.user u " +
           "WHERE b.user = :user AND b.isDeleted = false")
    Page<BookingResponse> findBookingsByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT new atmin.controller.booking.dto.response.BookingResponse(" +
           "b.id, b.bookingDate, b.timeSlot, b.totalPrice, b.status, " +
           "c.courtName, cl.name, u.fullName) " +
           "FROM Booking b " +
           "JOIN b.court c " +
           "JOIN c.cluster cl " +
           "JOIN b.user u " +
           "WHERE b.id = :id")
    Optional<BookingResponse> findResponseById(@Param("id") Long id);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.court = :court " +
           "AND b.bookingDate = :bookingDate " +
           "AND b.timeSlot = :timeSlot " +
           "AND b.status <> 'CANCELLED' " +
           "AND b.isDeleted = false")
    boolean existsActiveBooking(@Param("court") Court court,
                                @Param("bookingDate") LocalDate bookingDate,
                                @Param("timeSlot") String timeSlot);
}

