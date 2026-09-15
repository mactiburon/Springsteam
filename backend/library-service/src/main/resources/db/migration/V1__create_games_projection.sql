CREATE TABLE games (
    id           BIGINT PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    description  VARCHAR(5000),
    genre        VARCHAR(100),
    release_date DATE,
    developer    VARCHAR(200),
    publisher    VARCHAR(200),
    cover        VARCHAR(500),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);