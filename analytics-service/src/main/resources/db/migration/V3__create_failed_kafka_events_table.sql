create sequence failed_kafka_event_id_seq start with 1 increment by 50;

create table failed_kafka_events
(
    id                 bigint default nextval('failed_kafka_event_id_seq') not null,
    event_id           text,
    event_type         text                                                not null,
    topic              text                                                not null,
    partition_id       integer                                             not null,
    offset_value       bigint                                              not null,
    message_key        text,
    exception_message  text,
    payload            text                                                not null,
    failed_at          timestamp                                           not null,
    created_at         timestamp                                           not null,
    primary key (id)
);

create index idx_failed_kafka_events_event_id on failed_kafka_events(event_id);
create index idx_failed_kafka_events_topic on failed_kafka_events(topic);
create index idx_failed_kafka_events_failed_at on failed_kafka_events(failed_at);