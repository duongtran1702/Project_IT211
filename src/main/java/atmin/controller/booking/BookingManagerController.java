package atmin.controller.booking;

import atmin.common.response.ApiResponse;
import atmin.controller.booking.dto.request.BookingStatusUpdateRequest;
import atmin.controller.booking.dto.response.BookingResponse;
import atmin.service.IBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manager/bookings")
@RequiredArgsConstructor
public class BookingManagerController {

    private final IBookingService bookingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getBookings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("bookingDate").descending().and(Sort.by("id").descending()));
        Page<BookingResponse> bookings = bookingService.getBookings(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", bookings));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingStatusUpdateRequest request) {
        BookingResponse updatedBooking = bookingService.updateBookingStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Booking status updated successfully", updatedBooking));
    }
}
