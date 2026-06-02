# JobTrackr Database Design

## Overview

The initial version of JobTrackr will use a single table called `applications`.

This table will store all information related to a job application, including company details, application status, deadlines, notes, and referral information.

---

## Applications Table

| Column Name   | Data Type    | Description                    |
| ------------- | ------------ | ------------------------------ |
| id            | BIGINT       | Unique identifier              |
| company       | VARCHAR(100) | Company name                   |
| role          | VARCHAR(150) | Job title                      |
| location      | VARCHAR(100) | Job location                   |
| status        | VARCHAR(50)  | Current application status     |
| date_applied  | DATE         | Date application was submitted |
| deadline      | DATE         | Application deadline           |
| job_link      | VARCHAR(500) | Link to job posting            |
| notes         | TEXT         | Additional notes               |
| referral_name | VARCHAR(100) | Referral contact name          |

---

## Application Status Values

The following status values will be used:

* APPLIED
* ASSESSMENT
* INTERVIEW
* FINAL_INTERVIEW
* OFFER
* REJECTED
* WITHDRAWN

---

## Entity Relationship Diagram (ERD)

```text
+------------------------------------------------+
|                  applications                  |
+------------------------------------------------+
| id (PK)                                        |
| company                                        |
| role                                           |
| location                                       |
| status                                         |
| date_applied                                   |
| deadline                                       |
| job_link                                       |
| notes                                          |
| referral_name                                  |
+------------------------------------------------+
```

---

## Future Enhancements

The following fields may be added in future versions:

| Field              | Purpose                             |
| ------------------ | ----------------------------------- |
| resume_file_url    | AWS S3 resume storage               |
| resume_match_score | AI resume analysis                  |
| interview_date     | Interview tracking                  |
| follow_up_date     | Reminder tracking                   |
| application_source | LinkedIn, Referral, Company Website |
| salary_range       | Expected salary                     |
| interview_notes    | Interview feedback                  |

---

## Design Decision

A single-table design was selected for Version 1 to keep the application simple and focused on core functionality.

As the project evolves, additional tables such as Users, Resumes, Interviews, and Notifications may be introduced to support advanced features and improve scalability.
