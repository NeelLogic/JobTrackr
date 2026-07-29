import { ApplicationStatus, JobApplication } from './application.models';

export interface DashboardSummary {
  totalApplications: number;
  applicationsThisMonth: number;
  interviews: number;
  offers: number;
  rejections: number;
  activeApplications: number;
  overdueFollowUps: number;
  upcomingFollowUps: number;
  staleApplications: number;
  responseRate: number;
  interviewRate: number;
  offerRate: number;
  applicationsByStatus: Record<ApplicationStatus, number>;
  recentApplications: JobApplication[];
}
