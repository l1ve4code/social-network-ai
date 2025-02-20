--liquibase formatted sql

--changeset live4code:create-table-client_raw_images_processing_queue
create table if not exists social_network.client_raw_images_processing_queue (
    transaction_id        bigint                            generated always as identity,
    generation_id         bigint                            not null,
    client_id             bigint                            not null,
    client_tariff_id      bigint                            not null,
    theme_id              bigint                            not null,
    in_queue_time         timestamptz                       not null default current_timestamp,
    processed_time        timestamptz,
    constraint pk_client_raw_images_processing_queue primary key (transaction_id, client_id, theme_id)
);