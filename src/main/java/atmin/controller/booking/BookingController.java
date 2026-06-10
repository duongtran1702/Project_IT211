package atmin.controller.booking;

import atmin.common.response.ApiResponse;
import atmin.controller.booking.dto.request.BookingRequest;
import atmin.controller.booking.dto.response.BookingResponse;
import atmin.service.IBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final IBookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@Valid @RequestBody BookingRequest request) {
        BookingResponse booking = bookingService.createBooking(request);
        return new ResponseEntity<>(ApiResponse.success("Booking created successfully", booking), HttpStatus.CREATED);
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getBookingHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("bookingDate").descending().and(Sort.by("id").descending()));
        Page<BookingResponse> history = bookingService.getBookingHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success("Booking history retrieved successfully", history));
    }
}
