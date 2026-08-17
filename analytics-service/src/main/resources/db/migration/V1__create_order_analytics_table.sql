create sequence order_analytics_id_seq start with 1 increment by 50;

create table order_analytics
(
    id                  bigint default nextval('order_analytics_id_seq') not null,
    total_orders        bigint                                           not null default 0,
    total_items         bigint                                           not null default 0,
    created_at          timestamp                                        not null,
    updated_at          timestamp,
    primary key (id)
);

insert into order_analytics(id, total_orders, total_items, created_at, updated_at)
values (nextval('order_analytics_id_seq'), 0, 0, now(), now());