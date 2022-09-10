create table users
(
    id        uuid
        constraint users_pk
            primary key,
    email     text not null,
    firstname text not null,
    lastname  text not null,
    type      int  not null,
    birthday  date
);

create unique index users_email_uindex
    on users (email);

create unique index users_id_uindex
    on users (id);

create table groups
(
    id       int
        constraint groups_pk
            primary key,
    name     text not null,
    referent uuid references users,
    parent int references groups,
    private  bool not null
);

create unique index groups_id_uindex
    on groups (id);

create table users_groups
(
    id uuid references users,
    ref int references groups
)