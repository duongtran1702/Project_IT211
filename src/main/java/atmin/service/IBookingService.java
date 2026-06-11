package atmin.service;

import atmin.controller.booking.dto.request.BookingRequest;
import atmin.controller.booking.dto.response.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IBookingService {
    BookingResponse createBooking(BookingRequest request);
    Page<BookingResponse> getBookingHistory(Pageable pageable);
    Page<BookingResponse> getBookings(String status, Pageable pageable);
    BookingResponse updateBookingStatus(Long id, String status);
}
