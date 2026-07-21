import { ApplicationStatus, JobApplication } from './application.models';

export interface DashboardSummary {
  totalApplications: number;
  applicationsThisMonth: number;
  interviews: number;
  offers: number;
  rejections: number;
  applicationsByStatus: Record<ApplicationStatus, number>;
  recentApplications: JobApplication[];
}
