create table groups_tags
(
    id    int  not null references groups,
    key   text not null,
    value text not null,
    primary key (id, key)
);

create table users_tags
(
    id    uuid not null references users,
    key   text not null,
    value text not null,
    primary key (id, key)
);
