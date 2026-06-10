package atmin.controller.booking.dto.response;

import atmin.entity.Booking;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long id;
    private LocalDate bookingDate;
    private String timeSlot;
    private BigDecimal totalPrice;
    private String status;
    private String courtName;
    private String clusterName;
    private String customerFullName;

    public static BookingResponse fromEntity(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingDate(booking.getBookingDate())
                .timeSlot(booking.getTimeSlot())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .courtName(booking.getCourt() != null ? booking.getCourt().getCourtName() : null)
                .clusterName(booking.getCourt() != null && booking.getCourt().getCluster() != null ?
                        booking.getCourt().getCluster().getName() : null)
                .customerFullName(booking.getUser() != null ? booking.getUser().getFullName() : null)
                .build();
    }
}
