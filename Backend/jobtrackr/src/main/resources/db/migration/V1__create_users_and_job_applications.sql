CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE job_applications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    company VARCHAR(120) NOT NULL,
    job_title VARCHAR(160) NOT NULL,
    location VARCHAR(160),
    job_url VARCHAR(1000),
    application_date DATE,
    status VARCHAR(20) NOT NULL,
    employment_type VARCHAR(20) NOT NULL,
    salary_min DECIMAL(12, 2),
    salary_max DECIMAL(12, 2),
    salary_currency VARCHAR(3),
    notes VARCHAR(10000),
    follow_up_date DATE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_job_applications PRIMARY KEY (id),
    CONSTRAINT fk_job_applications_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_salary_range
        CHECK (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)
);

CREATE INDEX idx_app_user_status ON job_applications(user_id, status);
CREATE INDEX idx_app_user_date ON job_applications(user_id, application_date);
CREATE INDEX idx_app_user_updated ON job_applications(user_id, updated_at);
