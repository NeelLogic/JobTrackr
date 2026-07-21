export type ApplicationStatus = 'SAVED' | 'APPLIED' | 'ASSESSMENT' | 'INTERVIEW' | 'OFFER' | 'REJECTED' | 'WITHDRAWN';
export type EmploymentType = 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERNSHIP' | 'CO_OP' | 'TEMPORARY' | 'OTHER';

export const APPLICATION_STATUSES: ApplicationStatus[] = ['SAVED', 'APPLIED', 'ASSESSMENT', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN'];
export const EMPLOYMENT_TYPES: EmploymentType[] = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP', 'CO_OP', 'TEMPORARY', 'OTHER'];

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
  sort?: string;
  direction?: 'asc' | 'desc';
}
