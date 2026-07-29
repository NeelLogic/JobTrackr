package solannee.sheridancollege.ca.jobtrackr.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.AnalyticsRange;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.AnalyticsResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.CompaniesResponse;
import solannee.sheridancollege.ca.jobtrackr.dto.insights.FollowUpResponse;
import solannee.sheridancollege.ca.jobtrackr.model.User;
import solannee.sheridancollege.ca.jobtrackr.service.ApplicationInsightsService;
import solannee.sheridancollege.ca.jobtrackr.service.CurrentUserService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InsightsController {

    private final ApplicationInsightsService insightsService;
    private final CurrentUserService currentUserService;

    @GetMapping("/analytics")
    public AnalyticsResponse analytics(
            Authentication authentication,
            @RequestParam(defaultValue = "THIRTY_DAYS") String range
    ) {
        User user = currentUserService.require(authentication);
        return insightsService.analytics(user, AnalyticsRange.from(range));
    }

    @GetMapping("/companies")
    public CompaniesResponse companies(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "applications") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        User user = currentUserService.require(authentication);
        return insightsService.companies(user, search, sort, direction);
    }

    @GetMapping("/follow-ups")
    public FollowUpResponse followUps(Authentication authentication) {
        return insightsService.followUps(currentUserService.require(authentication));
    }
}
