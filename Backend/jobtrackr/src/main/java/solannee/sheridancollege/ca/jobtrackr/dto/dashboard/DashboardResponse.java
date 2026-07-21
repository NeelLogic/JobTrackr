package solannee.sheridancollege.ca.jobtrackr.dto.dashboard;
import solannee.sheridancollege.ca.jobtrackr.dto.application.ApplicationResponse;import java.util.*;
public record DashboardResponse(long totalApplications,long applicationsThisMonth,long interviews,long offers,long rejections,Map<String,Long>applicationsByStatus,List<ApplicationResponse>recentApplications){}
