package com.navan.commerce.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.navan.commerce.constant.ErrorResponseKeys;
import com.navan.commerce.domain.FeeKind;
import com.navan.commerce.domain.TransactionType;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.beans.TypeMismatchException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The only place error bodies are built. EVERY error response — business, validation, framework,
 * or unexpected — uses the same envelope:
 *
 * <pre>{
 *   "status": "FAILED",
 *   "error_code": &lt;http status int&gt;,
 *   "error_message": "&lt;specific reason, or 'Something went wrong'&gt;",
 *   "code": "&lt;machine-readable app code&gt;",        // plus context keys when relevant:
 *   "fields" / "existing_id" / "transaction_id" / "currencies": …
 * }</pre>
 *
 * No stack traces, no internals. Reasons are always included for 4xx (that is the point of 4xx);
 * 5xx gets only the generic message here and the full stack trace in the server log.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ------------------------------------------------------------------
    // Business errors raised by our own code
    // ------------------------------------------------------------------

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApi(ApiException ex) {
        log.warn("Request rejected: code={} detail={}", ex.getErrorCode().getCode(), ex.getMessage());
        return errorBody(ex.getErrorCode(), ex.getMessage(), ex.getDetails());
    }

    // ------------------------------------------------------------------
    // Validation failures — reason always included
    // ------------------------------------------------------------------

    /** Bean-validation failures on @RequestBody (missing external_id, bad currency, …). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        String summary = "Validation failed: " + String.join("; ",
                fields.stream().map(f -> f.get("field") + " " + f.get("message")).toList());
        return errorBody(ErrorCode.VALIDATION_FAILED, summary, Map.of(ErrorResponseKeys.FIELDS, fields));
    }

    /** Constraint violations on @RequestParam (limit=1000, offset=-1) → 400 (D5). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, String>> fields = ex.getConstraintViolations().stream()
                .map(v -> Map.of("field", v.getPropertyPath().toString(), "message", v.getMessage()))
                .toList();
        String summary = "Validation failed: " + String.join("; ",
                fields.stream().map(f -> f.get("field") + " " + f.get("message")).toList());
        return errorBody(ErrorCode.VALIDATION_FAILED, summary, Map.of(ErrorResponseKeys.FIELDS, fields));
    }

    /** Spring 6.1 handler-level method validation (e.g. @Min/@Max on params) — same shape as ConstraintViolation. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        String summary = "Validation failed: " + String.join("; ", ex.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(err -> String.valueOf(err.getDefaultMessage()))
                .toList());
        return errorBody(ErrorCode.VALIDATION_FAILED, summary, Map.of());
    }

    /** Malformed JSON, unknown type / fee_kind, unparseable dates, wrong metadata shape. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ErrorCode code = classifyParseError(ex);
        String reason = extractReason(ex);
        // For unknown type/fee_kind the reason already IS the message — don't double-prefix it.
        boolean reasonIsSelfContained = reason.startsWith("Unknown");
        String message = reasonIsSelfContained
                ? reason
                : code.getMessage() + (reason.isBlank() ? "" : ": " + reason);
        return errorBody(code, message, Map.of());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        return errorBody(ErrorCode.VALIDATION_FAILED,
                "Missing required parameter: " + ex.getParameterName(), Map.of());
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        return errorBody(ErrorCode.VALIDATION_FAILED,
                "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getPropertyName() + "'", Map.of());
    }

    // ------------------------------------------------------------------
    // Conflicts and framework fallbacks
    // ------------------------------------------------------------------

    /** Optimistic-lock loser on overlapping PATCH/DELETE (D10). */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return errorBody(ErrorCode.CONCURRENT_MODIFICATION, ErrorCode.CONCURRENT_MODIFICATION.getMessage(), Map.of());
    }

    /** Safety net for a constraint violation escaping the service (e.g. detected at commit). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex) {
        return errorBody(ErrorCode.CONCURRENT_MODIFICATION, ErrorCode.CONCURRENT_MODIFICATION.getMessage(), Map.of());
    }

    /** Unknown URL path → 404 in the same envelope. */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
                                                                    HttpHeaders headers,
                                                                    HttpStatusCode status,
                                                                    WebRequest request) {
        return errorBody(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found: " + ex.getResourcePath(), Map.of());
    }

    /** Catch-all: generic message to the client; the stack trace goes to the server log only. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return errorBody(ErrorCode.INTERNAL_ERROR, null, Map.of());
    }

    // ------------------------------------------------------------------
    // Envelope + parse classification helpers
    // ------------------------------------------------------------------

    /** The single envelope builder — status/FAILED + http error_code + message (never null/blank). */
    private ResponseEntity<Object> errorBody(HttpStatusCode status, String code, String message, Map<String, Object> extras) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(ErrorResponseKeys.STATUS, ErrorResponseKeys.STATUS_FAILED);
        body.put(ErrorResponseKeys.ERROR_CODE, status.value());
        body.put(ErrorResponseKeys.ERROR_MESSAGE,
                (message == null || message.isBlank()) ? ErrorResponseKeys.DEFAULT_ERROR_MESSAGE : message);
        body.put(ErrorResponseKeys.CODE, code);
        body.putAll(extras);
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<Object> errorBody(ErrorCode code, String message, Map<String, Object> extras) {
        return errorBody(code.getStatus(), code.getCode(), message, extras);
    }

    /** Maps Jackson parse failures to the spec's 400 contract (unknown type / unknown fee_kind). */
    private ErrorCode classifyParseError(HttpMessageNotReadableException ex) {
        for (Throwable t = ex.getCause(); t != null; t = t.getCause()) {
            if (t instanceof InvalidTypeIdException) {
                return ErrorCode.UNKNOWN_TYPE;
            }
            if (t instanceof InvalidFormatException fmt) {
                if (TransactionType.class.isAssignableFrom(fmt.getTargetType())) {
                    return ErrorCode.UNKNOWN_TYPE;
                }
                if (FeeKind.class.isAssignableFrom(fmt.getTargetType())) {
                    return ErrorCode.UNKNOWN_FEE_KIND;
                }
            }
            if (t instanceof IllegalArgumentException iae && iae.getMessage() != null) {
                if (iae.getMessage().startsWith("Unknown transaction type")) {
                    return ErrorCode.UNKNOWN_TYPE;
                }
                if (iae.getMessage().startsWith("Unknown fee_kind")) {
                    return ErrorCode.UNKNOWN_FEE_KIND;
                }
            }
        }
        return ErrorCode.VALIDATION_FAILED;
    }

    /** Extracts a client-safe, one-line reason from a Jackson failure chain (no class names). */
    private String extractReason(HttpMessageNotReadableException ex) {
        String fallback = "";
        for (Throwable t = ex.getCause(); t != null; t = t.getCause()) {
            if (t instanceof UnrecognizedPropertyException upe) {
                return "unknown field '" + upe.getPropertyName() + "'";
            }
            if (t instanceof InvalidTypeIdException ite) {
                return "unknown type id '" + ite.getTypeId() + "'";
            }
            if (t instanceof InvalidFormatException ife) {
                String field = ife.getPath().isEmpty() ? "request body"
                        : ife.getPath().get(ife.getPath().size() - 1).getFieldName();
                return "invalid value '" + ife.getValue() + "' for '" + field + "'";
            }
            // Our enums' @JsonCreator message ("Unknown transaction type: x") is the clean reason;
            // JsonMappingException wrapper text ("Cannot construct instance of `com.navan…`") is
            // kept only as last-resort fallback because it leaks the class FQCN.
            if (t instanceof IllegalArgumentException iae && iae.getMessage() != null) {
                return iae.getMessage();
            }
            if (fallback.isEmpty() && t instanceof JsonMappingException jme && jme.getOriginalMessage() != null) {
                fallback = jme.getOriginalMessage();
            }
        }
        return fallback.isEmpty() ? "malformed request body" : fallback;
    }

}
