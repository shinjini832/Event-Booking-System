package com.eventbooking.config;

import com.eventbooking.entity.AuditLog;
import com.eventbooking.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    @AfterReturning(
        pointcut = "execution(* com.eventbooking.service.BookingService.holdSeats(..)) || " +
                   "execution(* com.eventbooking.service.BookingService.confirmBooking(..)) || " +
                   "execution(* com.eventbooking.service.BookingService.cancelBooking(..))",
        returning = "result"
    )
    public void auditBookingStateChange(JoinPoint joinPoint, Object result) {
        String actor = getCurrentActorEmail();
        String methodName = joinPoint.getSignature().getName();

        try {
            AuditLog audit = AuditLog.builder()
                    .actorEmail(actor)
                    .action(methodName)
                    .entityType("BOOKING")
                    .details("Executed " + methodName + " successfully. Result: " + (result != null ? result.toString() : "null"))
                    .build();

            auditLogRepository.save(audit);
            log.info("Audit log persisted for action '{}' by actor '{}'", methodName, actor);
        } catch (Exception e) {
            log.error("Failed to persist audit log for {}: {}", methodName, e.getMessage());
        }
    }

    private String getCurrentActorEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM/ANONYMOUS";
    }
}
