package atmin.service.Impl;

import atmin.common.exception.DuplicateResourceException;
import atmin.common.exception.ResourceNotFoundException;
import atmin.controller.booking.dto.request.BookingRequest;
import atmin.controller.booking.dto.response.BookingResponse;
import atmin.entity.Booking;
import atmin.entity.Court;
import atmin.entity.TimeSlot;
import atmin.entity.User;
import atmin.repository.BookingRepository;
import atmin.repository.CourtRepository;
import atmin.repository.TimeSlotRepository;
import atmin.repository.UserRepository;
import atmin.service.IBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookingService implements IBookingService {

    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;

    private static final BigDecimal BASE_PRICE = new BigDecimal("100000");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        // 1. Lấy thông tin User hiện tại
        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Logged in user not found: " + username));

        // 2. Lấy thông tin Court
        Court court = courtRepository.findActiveById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Court not found or is unavailable: " + request.getCourtId()));

        // 3. Lấy thông tin TimeSlot
        TimeSlot timeSlotEntity = timeSlotRepository.findActiveById(request.getTimeSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found or is unavailable: " + request.getTimeSlotId()));

        // 4. Tạo chuỗi format timeSlot từ Entity (ví dụ: "07:00 - 09:00")
        String timeSlotString = timeSlotEntity.getStartTime().format(TIME_FORMATTER) + " - " +
                                timeSlotEntity.getEndTime().format(TIME_FORMATTER);

        // 5. Kiểm tra trùng lịch đặt
        boolean isBooked = bookingRepository.existsActiveBooking(court, request.getBookingDate(), timeSlotString);
        if (isBooked) {
            throw new DuplicateResourceException("This court has already been booked for this time slot on the selected date!");
        }

        // 6. Tính toán giá động
        BigDecimal totalPrice = BASE_PRICE.multiply(timeSlotEntity.getPriceFactor());

        // 7. Tạo và lưu Booking
        Booking booking = Booking.builder()
                .bookingDate(request.getBookingDate())
                .timeSlot(timeSlotString)
                .totalPrice(totalPrice)
                .status("PENDING") // Trạng thái mặc định ban đầu là PENDING
                .user(user)
                .court(court)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        return bookingRepository.findResponseById(savedBooking.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found after creation: " + savedBooking.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingHistory(Pageable pageable) {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Logged in user not found: " + username));

        return bookingRepository.findBookingsByUser(user, pageable);
    }
}
