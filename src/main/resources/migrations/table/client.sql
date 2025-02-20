--liquibase formatted sql

--changeset live4code:create-enum-roles
create type social_network.client_role as enum ('ROLE_USER', 'ROLE_ADMIN');

--changeset live4code:create-table-client
create table if not exists social_network.client (
    id         bigint                     generated always as identity,
    email      varchar                    not null,
    name       varchar                    ,
    surname    varchar                    ,
    password   varchar                    not null,
    enabled    boolean                    not null default false,
    role       social_network.client_role not null default 'ROLE_USER'::social_network.client_role,
    created_at timestamptz                not null default current_timestamp,
    constraint pk_client                  primary key (email)
);