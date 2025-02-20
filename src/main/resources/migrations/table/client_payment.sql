--liquibase formatted sql

--changeset live4code:create-enum-client_payment_status
create type social_network.client_payment_status as enum ('SUCCEEDED', 'PENDING', 'ERROR');

--changeset live4code:create-table-client_payment
create table if not exists social_network.client_payment (
    id        varchar                              not null primary key,
    client_id bigint                               not null,
    tariff_id bigint                               not null,
    amount    bigint                               not null,
    status    social_network.client_payment_status not null default 'PENDING'::social_network.client_payment_status
);