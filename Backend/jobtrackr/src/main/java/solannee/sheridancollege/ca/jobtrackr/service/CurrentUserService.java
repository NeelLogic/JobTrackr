package solannee.sheridancollege.ca.jobtrackr.service;
import lombok.RequiredArgsConstructor;import org.springframework.security.core.Authentication;import org.springframework.stereotype.Service;import solannee.sheridancollege.ca.jobtrackr.model.User;import solannee.sheridancollege.ca.jobtrackr.repository.UserRepository;
@Service @RequiredArgsConstructor public class CurrentUserService{private final UserRepository users;public User require(Authentication auth){return users.findByEmailIgnoreCase(auth.getName()).orElseThrow();}}
