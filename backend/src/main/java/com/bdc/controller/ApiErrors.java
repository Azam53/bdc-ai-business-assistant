package com.bdc.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@RestControllerAdvice public class ApiErrors {
 @ExceptionHandler(ResponseStatusException.class)
 public ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Request failed":e.getReason()));}
 @ExceptionHandler({java.time.format.DateTimeParseException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
 public ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("message","Invalid input. Use ISO dates (YYYY-MM-DD) and a valid JSON request."));}
 @ExceptionHandler(UnsupportedOperationException.class)
 public ResponseEntity<?> unconfigured(Exception e){return ResponseEntity.status(503).body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(Exception.class)
 public ResponseEntity<?> error(Exception e){return ResponseEntity.status(500).body(Map.of("message","The analysis could not be completed. Check server configuration and try again."));}
}
