--liquibase formatted sql

--changeset live4code:create-table-audience
create table if not exists social_network.audience (
    client_id         bigint  not null,
    need_subscribes   integer not null default 0,
    need_unsubscribes integer not null default 0,
    done_subscribes   integer not null default 0,
    done_unsubscribes integer not null default 0,
    processed_at      date    not null,
    constraint pk_audience primary key (client_id)
);