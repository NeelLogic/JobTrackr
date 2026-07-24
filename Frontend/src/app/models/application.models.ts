export const APPLICATION_STATUSES = [
  'SAVED',
  'APPLIED',
  'ASSESSMENT',
  'INTERVIEW',
  'OFFER',
  'REJECTED',
  'WITHDRAWN',
] as const;

export const EMPLOYMENT_TYPES = [
  'FULL_TIME',
  'PART_TIME',
  'CONTRACT',
  'INTERNSHIP',
  'CO_OP',
  'TEMPORARY',
  'OTHER',
] as const;

export const APPLICATION_SORT_FIELDS = [
  'company',
  'jobTitle',
  'applicationDate',
  'status',
  'createdAt',
  'updatedAt',
  'followUpDate',
] as const;

export type ApplicationStatus = (typeof APPLICATION_STATUSES)[number];
export type EmploymentType = (typeof EMPLOYMENT_TYPES)[number];
export type ApplicationSortField = (typeof APPLICATION_SORT_FIELDS)[number];
export type SortDirection = 'asc' | 'desc';

export interface JobApplication {
  id: number;
  company: string;
  jobTitle: string;
  location: string | null;
  jobUrl: string | null;
  applicationDate: string | null;
  status: ApplicationStatus;
  employmentType: EmploymentType;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
  notes: string | null;
  followUpDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export type ApplicationRequest = Omit<JobApplication, 'id' | 'createdAt' | 'updatedAt'>;

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApplicationQuery {
  search?: string;
  status?: ApplicationStatus | '';
  employmentType?: EmploymentType | '';
  page?: number;
  size?: number;
  sort?: ApplicationSortField;
  direction?: SortDirection;
}
