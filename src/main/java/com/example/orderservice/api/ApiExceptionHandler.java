package com.example.orderservice.api;

import com.example.orderservice.application.OrderExceptions.BadOrderRequest;
import com.example.orderservice.application.OrderExceptions.IdempotencyConflict;
import com.example.orderservice.application.OrderExceptions.OrderNotFound;
import com.example.orderservice.integration.CatalogProductException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler({BadOrderRequest.class, CatalogProductException.class,
            MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ProblemDetail badRequest(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid order request", safeMessage(exception), request);
    }

    @ExceptionHandler(IdempotencyConflict.class)
    ProblemDetail conflict(IdempotencyConflict exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Idempotency conflict", exception.getMessage(), request);
    }

    @ExceptionHandler(OrderNotFound.class)
    ProblemDetail notFound(OrderNotFound exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Order not found", exception.getMessage(), request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail,
                                  HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }

    private String safeMessage(Exception exception) {
        return exception instanceof MethodArgumentNotValidException
                ? "Request validation failed"
                : exception.getMessage();
    }
}
