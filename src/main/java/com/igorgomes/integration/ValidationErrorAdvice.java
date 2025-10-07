package com.igorgomes.integration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global felhanterare för valideringsfel.
 * Översätter Bean Validation-undantag till HTTP 400 (Bad Request)
 * och returnerar strukturerat JSON enligt RFC 7807 (application/problem+json).
 * Detta ger konsekventa och maskinläsbara fel i alla miljöer.
 */
@RestControllerAdvice
public class ValidationErrorAdvice {

    /**
     * Hanterar metod-/parameter-validering (t.ex. @RequestParam)
     * som kastar {@link ConstraintViolationException}.
     * Returnerar HTTP 400 med JSON i formatet application/problem+json.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Valideringsfel");
        problem.setDetail("En eller flera parametrar är ogiltiga.");
        problem.setProperty("path", request.getRequestURI());

        // Extrahera endast sista noden i propertyPath (t.ex. "sendMessage.message" -> "message")
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(v -> {
                    String fullPath = v.getPropertyPath().toString();
                    String fieldName = fullPath.contains(".")
                            ? fullPath.substring(fullPath.lastIndexOf('.') + 1)
                            : fullPath;
                    return Map.of(
                            "field", fieldName,
                            "message", v.getMessage()
                    );
                })
                .toList();

        problem.setProperty("errors", errors);

        // Sätt explicit Content-Type för att undvika 406 (Not Acceptable)
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Hanterar kroppsbunden validering (t.ex. @Valid @RequestBody).
     * Returnerar HTTP 400 + JSON (application/problem+json) med fältfel.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Valideringsfel i begäran");
        problem.setDetail("En eller flera fält i begäran är ogiltiga.");
        problem.setProperty("path", request.getRequestURI());

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getDefaultMessage()))
                .collect(Collectors.toList());

        problem.setProperty("errors", errors);

        // (SV) Sätt explicit Content-Type för att undvika 406 (Not Acceptable)
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Hanterar saknad request-parameter (t.ex. 'message' saknas helt).
     * Returnerar HTTP 400 + JSON (application/problem+json) med tydlig beskrivning.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Saknad parameter");
        problem.setDetail("Parametern '" + ex.getParameterName() + "' är obligatorisk.");
        problem.setProperty("path", request.getRequestURI());

        // Sätt explicit Content-Type för att undvika 406 (Not Acceptable)
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
