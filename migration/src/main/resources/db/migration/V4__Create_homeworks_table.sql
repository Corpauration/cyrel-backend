create table homeworks
(
    id      uuid
        constraint homeworks_pk
            primary key,
    title   text not null,
    level   int  not null,
    content text not null,
    date    date not null,
    "group" int  not null references groups
);

create unique index homeworks_id_uindex
    on homeworks (id);