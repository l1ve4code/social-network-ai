--liquibase formatted sql

--changeset live4code:create-table-platform_workflow
create table if not exists social_network.platform_workflow (
    id          bigint   generated always as identity,
    title       varchar   not null,
    title_color varchar   not null,
    description varchar[] not null,
    link        varchar   not null
);