alter table order_events
    add column status text not null default 'PROCESSING';

alter table order_events
    add column processed_at timestamp;

alter table order_events
    add column last_error text;

alter table order_events
    add column retry_count integer not null default 0;

create index if not exists idx_order_events_status on order_events(status);
