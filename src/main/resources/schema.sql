DROP TABLE IF EXISTS external_users;

CREATE TABLE external_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    encoded_password VARCHAR(255) NOT NULL,
    roles VARCHAR(255) NOT NULL,
    permissions VARCHAR(512) NOT NULL
);
