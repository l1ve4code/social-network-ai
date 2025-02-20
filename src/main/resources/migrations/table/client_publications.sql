--liquibase formatted sql

--changeset live4code:create-table-client_publications
create table if not exists social_network.client_publications (
    id               bigint      not null generated always as identity,
    client_id        bigint      not null,
    client_tariff_id bigint      not null,
    post_id          bigint      not null,
    publish_at       timestamptz not null,
    published        boolean     not null default false,
    created_at       timestamptz not null default current_timestamp,
    constraint pk_client_publications primary key (client_id, post_id)
);