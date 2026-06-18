CREATE TABLE IF NOT EXISTS site_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(80),
    display_name VARCHAR(120),
    email VARCHAR(160),
    avatar VARCHAR(500),
    oauth_sub VARCHAR(160) NOT NULL UNIQUE,
    roles VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_site_users_email ON site_users(email);

CREATE TABLE IF NOT EXISTS sites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    slug VARCHAR(60) NOT NULL UNIQUE,
    framework VARCHAR(30) NOT NULL,
    description VARCHAR(240),
    status VARCHAR(30) NOT NULL,
    default_domain VARCHAR(253) NOT NULL,
    custom_domain VARCHAR(253),
    preview_image VARCHAR(500),
    branch_name VARCHAR(80),
    source_filename VARCHAR(255),
    source_path VARCHAR(1000),
    latest_deployment_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sites_user_id ON sites(user_id);

CREATE TABLE IF NOT EXISTS deployments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    environment VARCHAR(30),
    commit_hash VARCHAR(32),
    source_filename VARCHAR(255),
    output_path VARCHAR(1000),
    build_log CLOB,
    duration_seconds INTEGER,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_deployments_user_created ON deployments(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_deployments_site_created ON deployments(site_id, created_at);

CREATE TABLE IF NOT EXISTS site_domains (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    domain VARCHAR(253) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    verification_token VARCHAR(120),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_domains_user_id ON site_domains(user_id);

