--liquibase formatted sql

--changeset live4code:create-table-refresh_token
create table if not exists social_network.refresh_token (
    token      varchar               not null primary key,
    client_id  bigint                not null,
    expired_at timestamptz           not null default current_timestamp,
    created_at timestamptz           not null default current_timestamp
);