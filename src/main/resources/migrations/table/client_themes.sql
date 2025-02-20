--liquibase formatted sql

--changeset live4code:create-table-client_themes
create table if not exists social_network.client_themes (
    id                   bigint           generated always as identity primary key,
    generation_id        bigint           not null,
    client_id            bigint           not null,
    client_tariff_id     bigint           not null,
    direction_id         bigint           not null,
    text                 varchar          not null,
    approved             boolean          not null default false,
    created_at           timestamptz      not null default current_timestamp
);