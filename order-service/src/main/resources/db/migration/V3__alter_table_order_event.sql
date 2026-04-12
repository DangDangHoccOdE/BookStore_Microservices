alter table order_events
    add column status text not null default 'PENDING';

alter table order_events
    add column locked_by text;

alter table order_events
    add column locked_at timestamp;

alter table order_events
    add column published_at timestamp;

alter table order_events
    add column retry_count integer not null default 0;
