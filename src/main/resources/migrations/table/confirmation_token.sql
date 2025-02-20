--liquibase formatted sql

--changeset live4code:create-table-confirmation_token
create table if not exists social_network.confirmation_token (
    id         bigint                generated always as identity,
    value      varchar               not null,
    client_id  bigint                not null,
    created_at timestamptz           not null default current_timestamp,
    constraint pk_confirmation_token primary key (value)
);