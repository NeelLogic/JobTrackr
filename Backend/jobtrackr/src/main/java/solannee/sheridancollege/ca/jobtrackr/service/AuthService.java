package solannee.sheridancollege.ca.jobtrackr.service;
import lombok.RequiredArgsConstructor; import org.springframework.security.authentication.*; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import solannee.sheridancollege.ca.jobtrackr.dto.auth.*; import solannee.sheridancollege.ca.jobtrackr.exception.ConflictException; import solannee.sheridancollege.ca.jobtrackr.model.User; import solannee.sheridancollege.ca.jobtrackr.repository.UserRepository; import solannee.sheridancollege.ca.jobtrackr.security.JwtService;
@Service @RequiredArgsConstructor public class AuthService {
 private final UserRepository users;private final PasswordEncoder encoder;private final AuthenticationManager authenticationManager;private final JwtService jwt;
 @Transactional public AuthResponse register(RegisterRequest req){String email=req.email().trim().toLowerCase();if(users.existsByEmailIgnoreCase(email))throw new ConflictException("An account with this email already exists");User u=new User();u.setName(req.name().trim());u.setEmail(email);u.setPasswordHash(encoder.encode(req.password()));return response(users.save(u));}
 public AuthResponse login(LoginRequest req){authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email().trim().toLowerCase(),req.password()));User u=users.findByEmailIgnoreCase(req.email()).orElseThrow();return response(u);}
 private AuthResponse response(User u){return new AuthResponse(jwt.generateToken(u.getEmail()),"Bearer",jwt.getExpirationSeconds(),new UserResponse(u.getId(),u.getName(),u.getEmail()));}
}
