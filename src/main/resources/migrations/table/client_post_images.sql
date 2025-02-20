--liquibase formatted sql

--changeset live4code:create-table-client_post_images
create table if not exists social_network.client_post_images (
    id                   varchar               not null primary key,
    generation_id        bigint                not null,
    client_id            bigint                not null,
    client_tariff_id     bigint                not null,
    theme_id             bigint                not null,
    is_used              boolean               not null,
    created_at           timestamptz           not null default current_timestamp
);