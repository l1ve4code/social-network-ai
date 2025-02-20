--liquibase formatted sql

--changeset live4code:create-schema-social_network
create schema if not exists social_network;