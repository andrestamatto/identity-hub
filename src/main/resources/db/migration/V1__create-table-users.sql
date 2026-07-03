CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    username_type VARCHAR(255) NOT NULL,
    encoded_password VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    verification_token_code VARCHAR(255),
    verification_token_method VARCHAR(255),
    verification_token_expires_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_users_username UNIQUE (username)
);
