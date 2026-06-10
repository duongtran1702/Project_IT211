package atmin.common.aspect;

import atmin.controller.booking.dto.request.BookingRequest;
import atmin.controller.booking.dto.response.BookingResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @AfterReturning(
            pointcut = "execution(* atmin.service.IBookingService.createBooking(..))",
            returning = "result"
    )
    public void logBookingSuccess(Object result) {
        if (result instanceof BookingResponse response) {
            log.info("[AUDIT - SUCCESS] Khách hàng {} đặt thành công {} vào ngày {}, Khung giờ {}.",
                    response.getCustomerFullName(),
                    response.getCourtName(),
                    response.getBookingDate(),
                    response.getTimeSlot());
        }
    }

    @AfterThrowing(
            pointcut = "execution(* atmin.service.IBookingService.createBooking(..))",
            throwing = "ex"
    )
    public void logBookingFailure(JoinPoint joinPoint, Throwable ex) {
        String username = "Anonymous";
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        Long courtId = null;
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof BookingRequest request) {
            courtId = request.getCourtId();
        }

        log.error("[AUDIT - FAILED] Khách hàng {} cố gắng đặt Sân số {} nhưng thất bại do {}.",
                username,
                courtId != null ? courtId : "không xác định",
                ex.getMessage());
    }
}
