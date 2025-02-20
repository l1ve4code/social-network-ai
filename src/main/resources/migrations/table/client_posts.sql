--liquibase formatted sql

--changeset live4code:create-table-client_posts
create table if not exists social_network.client_posts (
    id                   bigint      generated always as identity primary key,
    client_id            bigint      not null,
    client_tariff_id     bigint      not null,
    theme_id             bigint      not null,
    text                 varchar     not null,
    approved             boolean     not null default false,
    created_at           timestamptz not null default current_timestamp
);