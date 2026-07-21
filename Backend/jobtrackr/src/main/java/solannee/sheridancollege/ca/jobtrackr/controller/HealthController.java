package solannee.sheridancollege.ca.jobtrackr.controller;
import org.springframework.web.bind.annotation.*;import java.util.Map;
@RestController public class HealthController{@GetMapping("/api/health")Map<String,String>health(){return Map.of("status","UP");}}
