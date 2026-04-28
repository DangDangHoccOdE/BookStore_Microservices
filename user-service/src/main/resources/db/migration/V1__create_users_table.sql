create table users (
                       id uuid primary key,
                       keycloak_user_id varchar(64) not null unique,
                       username varchar(100) not null unique,
                       email varchar(320) not null unique,
                       status varchar(20) not null,
                       created_at timestamptz not null,
                       updated_at timestamptz not null
);
