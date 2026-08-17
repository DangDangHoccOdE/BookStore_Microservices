create sequence processed_event_id_seq start with 1 increment by 50;

create table processed_events
(
    id             bigint default nextval('processed_event_id_seq') not null,
    event_id       text                                             not null,
    event_type     text                                             not null,
    processed_at   timestamp                                        not null,
    created_at     timestamp                                        not null,
    primary key (id),
    constraint uk_processed_events_event_id unique (event_id)
);

create index idx_processed_events_event_type on processed_events(event_type);
create index idx_processed_events_processed_at on processed_events(processed_at);