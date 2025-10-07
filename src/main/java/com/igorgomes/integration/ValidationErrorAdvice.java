package com.igorgomes.integration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.List;
import java.util.Locale;
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

    private final MessageSource messageSource;

    public ValidationErrorAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

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

        Locale locale = RequestContextUtils.getLocale(request);

        // Extrahera endast sista noden i propertyPath (t.ex. "sendMessage.message" -> "message")
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(v -> {
                    String fullPath = v.getPropertyPath().toString();
                    String fieldName = fullPath.contains(".")
                            ? fullPath.substring(fullPath.lastIndexOf('.') + 1)
                            : fullPath;

                    // v.getMessage() kan vara interpolerad eller en mall som "{message.*}"
                    String raw = v.getMessage();
                    String resolved = resolveIfTemplate(raw, locale);

                    return Map.of(
                            "field", fieldName,
                            "message", resolved
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

        Locale locale = RequestContextUtils.getLocale(request);

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", resolveFieldErrorMessage(err, locale)))
                .collect(Collectors.toList());

        problem.setProperty("errors", errors);

        // Sätt explicit Content-Type för att undvika 406 (Not Acceptable)
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

    //  Hjälpmetoder för att säkert lösa lokaliserade meddelanden ---

    private String resolveFieldErrorMessage(FieldError err, Locale locale) {
        // Bean Validation brukar redan interpolera defaultMessage; om inte, försök via MessageSource.
        String dm = err.getDefaultMessage();
        if (dm != null && !dm.startsWith("{")) {
            return dm;
        }
        try {
            return messageSource.getMessage(err, locale);
        } catch (NoSuchMessageException e) {
            return fallbackFromTemplate(dm);
        }
    }

    private String resolveIfTemplate(String msg, Locale locale) {
        if (msg == null) return "Ogiltig parameter.";
        if (!msg.startsWith("{")) return msg;

        String key = msg.substring(1, msg.length() - 1); // "{message.required}" -> "message.required"
        try {
            return messageSource.getMessage(key, null, locale);
        } catch (NoSuchMessageException e) {
            return fallbackFromTemplate(msg);
        }
    }

    private String fallbackFromTemplate(String templateOrNull) {
        // En försiktig fallback om nyckeln saknas – returnera antingen råsträngen eller en neutral text.
        return (templateOrNull != null) ? templateOrNull : "Ogiltig parameter.";
    }
}
