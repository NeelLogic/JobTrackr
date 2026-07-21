package solannee.sheridancollege.ca.jobtrackr.exception;
import jakarta.servlet.http.HttpServletRequest;import org.springframework.dao.DataIntegrityViolationException;import org.springframework.http.*;import org.springframework.security.core.AuthenticationException;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.*;import java.time.Instant;import java.util.*;
@RestControllerAdvice public class GlobalExceptionHandler{
 @ExceptionHandler(ResourceNotFoundException.class)ResponseEntity<ApiError>notFound(RuntimeException e,HttpServletRequest r){return error(HttpStatus.NOT_FOUND,e.getMessage(),r,null);}
 @ExceptionHandler(ConflictException.class)ResponseEntity<ApiError>conflict(RuntimeException e,HttpServletRequest r){return error(HttpStatus.CONFLICT,e.getMessage(),r,null);}
 @ExceptionHandler({IllegalArgumentException.class,DataIntegrityViolationException.class})ResponseEntity<ApiError>bad(RuntimeException e,HttpServletRequest r){return error(HttpStatus.BAD_REQUEST,e instanceof DataIntegrityViolationException?"Request violates a data constraint":e.getMessage(),r,null);}
 @ExceptionHandler(AuthenticationException.class)ResponseEntity<ApiError>auth(AuthenticationException e,HttpServletRequest r){return error(HttpStatus.UNAUTHORIZED,"Invalid email or password",r,null);}
 @ExceptionHandler(MethodArgumentNotValidException.class)ResponseEntity<ApiError>validation(MethodArgumentNotValidException e,HttpServletRequest r){Map<String,String>fields=new LinkedHashMap<>();e.getBindingResult().getFieldErrors().forEach(x->fields.putIfAbsent(x.getField(),x.getDefaultMessage()));return error(HttpStatus.BAD_REQUEST,"Validation failed",r,fields);}
 @ExceptionHandler(Exception.class)ResponseEntity<ApiError>other(Exception e,HttpServletRequest r){return error(HttpStatus.INTERNAL_SERVER_ERROR,"An unexpected error occurred",r,null);}
 private ResponseEntity<ApiError>error(HttpStatus s,String m,HttpServletRequest r,Map<String,String>f){return ResponseEntity.status(s).body(new ApiError(Instant.now(),s.value(),s.getReasonPhrase(),m,r.getRequestURI(),f));}
}
