--liquibase formatted sql

--changeset live4code:create-table-client_directions
create table if not exists social_network.client_directions (
    id                bigint               generated always as identity,
    client_id         bigint               not null,
    client_tariff_id  bigint               not null,
    text              varchar              not null,
    created_at        timestamptz          not null default current_timestamp,
    constraint pk_client_directions primary key (client_id, client_tariff_id)
);