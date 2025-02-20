--liquibase formatted sql

--changeset live4code:create-table-tariff
create table if not exists social_network.tariff (
    id                       bigint  generated always as identity primary key,
    name                     varchar not null,
    discount_price           bigint  not null,
    price                    bigint  not null,
    publication_amount       integer not null default 0,
    subscribes_per_day       integer not null default 0,
    unsubscribes_per_day     integer not null default 0,
    is_promo                 boolean not null default false
);