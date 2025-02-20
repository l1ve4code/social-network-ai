--liquibase formatted sql

--changeset live4code:create-table-tenchat_credentials
create table if not exists social_network.tenchat_credentials (
    client_id     bigint      not null,
    phone         varchar     not null,
    access_token  varchar     not null,
    refresh_token varchar     not null,
    updated_at    timestamptz not null default current_timestamp,
    created_at    timestamptz not null default current_timestamp,
    constraint pk_tenchat_credentials primary key (client_id)
);