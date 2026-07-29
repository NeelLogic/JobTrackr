package solannee.sheridancollege.ca.jobtrackr.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solannee.sheridancollege.ca.jobtrackr.dto.dashboard.DashboardResponse;
import solannee.sheridancollege.ca.jobtrackr.service.ApplicationInsightsService;
import solannee.sheridancollege.ca.jobtrackr.service.CurrentUserService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ApplicationInsightsService insightsService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public DashboardResponse get(Authentication authentication) {
        return insightsService.dashboard(currentUserService.require(authentication));
    }
}
