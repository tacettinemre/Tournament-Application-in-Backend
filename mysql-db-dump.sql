CREATE TABLE tournaments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_tournament_is_active (is_active)
);


CREATE TABLE user_groups (
    group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id BIGINT,
    group_status VARCHAR(20),
    countries VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (tournament_id) REFERENCES tournaments(id),
    INDEX idx_group_tournament_id (tournament_id)
);



CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coins INT NOT NULL DEFAULT 5000,
    level INT NOT NULL DEFAULT 1,
    country VARCHAR(50) NOT NULL,
    group_id BIGINT,
    score INT NOT NULL DEFAULT 0,
    hasReward INT NOT NULL DEFAULT 0,
    FOREIGN KEY (group_id) REFERENCES user_groups(group_id),
    INDEX idx_user_group_id (group_id),
); 
