package app; // O app.config, o app.exceptions

import app.exceptions.InvalidOperationException;
import app.exceptions.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException; // Para errores de validación
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger; // Para logging
import org.slf4j.LoggerFactory; // Para logging

import java.util.HashMap; // Para formatear errores de validación
import java.util.Map; // Para formatear errores de validación
import java.util.stream.Collectors; // Para formatear errores de validación


@RestControllerAdvice // Captura excepciones de todos los @RestController
public class GlobalExceptionHandler {

    // Logger para registrar errores
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Manejador para ResourceNotFoundException (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage()); // Log como advertencia
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // Manejador para InvalidOperationException (400)
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<Map<String, String>> handleInvalidOperationException(InvalidOperationException ex) {
        log.warn("Invalid operation requested: {}", ex.getMessage()); // Log como advertencia
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Manejador para errores de validación (@Valid) (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Validation Failed");
        // Extraer los mensajes de error por campo
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value"
                ));
        errorResponse.put("fieldErrors", fieldErrors);
        errorResponse.put("message", "Validation error(s) occurred. Check 'fieldErrors' for details.");
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }


    // Manejador genérico para cualquier otra excepción (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("An unexpected error occurred: ", ex); // Log como error con stack trace
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal Server Error");
        // No exponer detalles internos al cliente en producción por seguridad
        // errorResponse.put("message", ex.getClass().getSimpleName() + " - " + ex.getMessage());
        errorResponse.put("message", "An unexpected error occurred. Please try again later.");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}