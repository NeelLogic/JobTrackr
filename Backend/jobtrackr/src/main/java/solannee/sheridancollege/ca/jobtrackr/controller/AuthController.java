package solannee.sheridancollege.ca.jobtrackr.controller;
import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import solannee.sheridancollege.ca.jobtrackr.dto.auth.*;import solannee.sheridancollege.ca.jobtrackr.service.AuthService;
@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor public class AuthController{
 private final AuthService service;
 @PostMapping("/register") ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.register(r));}
 @PostMapping("/login") AuthResponse login(@Valid @RequestBody LoginRequest r){return service.login(r);}
}
