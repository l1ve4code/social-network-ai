--liquibase formatted sql

--changeset live4code:create-table-environment
create table if not exists social_network.environment (
    key   varchar not null,
    value varchar not null
);