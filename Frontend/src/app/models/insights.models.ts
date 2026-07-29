import { ApplicationStatus, EmploymentType, JobApplication } from './application.models';

export const ANALYTICS_RANGES = ['THIRTY_DAYS', 'NINETY_DAYS', 'SIX_MONTHS', 'ALL_TIME'] as const;

export type AnalyticsRange = (typeof ANALYTICS_RANGES)[number];
export type CompanySort = 'applications' | 'company' | 'interviews' | 'offers' | 'recent';

export interface FunnelStage {
  stage: ApplicationStatus;
  applications: number;
  conversionFromApplied: number;
}

export interface TrendPoint {
  periodStart: string;
  applications: number;
}

export interface CompanyInsight {
  company: string;
  totalApplications: number;
  activeApplications: number;
  interviewsReached: number;
  offersReached: number;
  latestApplicationDate: string | null;
  lastActivityAt: string;
}

export interface AnalyticsSummary {
  range: AnalyticsRange;
  fromDate: string | null;
  toDate: string;
  applicationsInRange: number;
  previousPeriodApplications: number;
  applicationGrowthRate: number;
  responseRate: number;
  interviewRate: number;
  offerRate: number;
  funnel: FunnelStage[];
  trend: TrendPoint[];
  applicationsByStatus: Record<ApplicationStatus, number>;
  applicationsByEmploymentType: Record<EmploymentType, number>;
  topCompanies: CompanyInsight[];
}

export interface CompaniesSummary {
  totalCompanies: number;
  companies: CompanyInsight[];
}

export interface CompaniesQuery {
  search?: string;
  sort?: CompanySort;
  direction?: 'asc' | 'desc';
}

export interface FollowUpSummary {
  overdueCount: number;
  dueTodayCount: number;
  upcomingCount: number;
  staleCount: number;
  overdue: JobApplication[];
  dueToday: JobApplication[];
  upcoming: JobApplication[];
  stale: JobApplication[];
}
