--liquibase formatted sql

--changeset live4code:create-table-recover_token
create table if not exists social_network.recover_token (
    token      varchar               not null primary key,
    client_id  bigint                not null,
    expired_at timestamptz           not null,
    created_at timestamptz           not null default current_timestamp
);