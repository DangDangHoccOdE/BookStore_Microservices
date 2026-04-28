create table user_profiles (
                               user_id uuid primary key references users(id) on delete cascade,
                               first_name varchar(100),
                               last_name varchar(100),
                               phone varchar(30),
                               avatar_url text,
                               metadata jsonb not null default '{}'::jsonb,
                               updated_at timestamptz not null
);
