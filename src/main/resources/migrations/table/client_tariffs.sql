--liquibase formatted sql

--changeset live4code:create-table-client_tariffs
create table if not exists social_network.client_tariffs (
    id           bigint      generated always as identity,
    client_id    bigint      not null,
    tariff_id    bigint      not null,
    start_date   date        not null,
    end_date     date        not null,
    created_at timestamptz not null default current_timestamp
);