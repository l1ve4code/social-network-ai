--liquibase formatted sql

--changeset live4code:create-schema-quartz
create schema if not exists quartz;