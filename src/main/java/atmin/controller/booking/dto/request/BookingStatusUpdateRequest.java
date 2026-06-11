package atmin.controller.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatusUpdateRequest {
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(CONFIRMED|REJECTED)$", message = "Status must be either CONFIRMED or REJECTED")
    private String status;
}
