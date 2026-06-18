CREATE TABLE IF NOT EXISTS site_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(80),
    display_name VARCHAR(120),
    email VARCHAR(160),
    avatar VARCHAR(500),
    oauth_sub VARCHAR(160) NOT NULL,
    roles VARCHAR(1000),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_site_users_oauth_sub (oauth_sub),
    UNIQUE KEY uk_site_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    slug VARCHAR(60) NOT NULL,
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
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_sites_slug (slug),
    KEY idx_sites_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS deployments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    site_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    environment VARCHAR(30),
    commit_hash VARCHAR(32),
    source_filename VARCHAR(255),
    output_path VARCHAR(1000),
    build_log LONGTEXT,
    duration_seconds INT,
    started_at DATETIME,
    finished_at DATETIME,
    created_at DATETIME NOT NULL,
    KEY idx_deployments_user_created (user_id, created_at),
    KEY idx_deployments_site_created (site_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS site_domains (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    site_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    domain VARCHAR(253) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    verification_token VARCHAR(120),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_site_domains_domain (domain),
    KEY idx_domains_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

