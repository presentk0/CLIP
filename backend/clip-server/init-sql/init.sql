CREATE TABLE IF NOT EXISTS user (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    email VARCHAR(255) NOT NULL,
    name VARCHAR(20) NOT NULL,
    profile_image_url VARCHAR(500),
    level INT DEFAULT 1,
    exp INT DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME
    );

INSERT INTO user (email, name, profile_image_url, level, exp, created_at, updated_at)
VALUES ('test@test.com', '홍길동', NULL, 1, 0, NOW(), NOW());