create table themes
(
    id         int  not null
        primary key
        unique,
    background text not null,
    foreground text not null,
    card       text not null,
    "navIcon"  text not null
);

create table preferences
(
    id    uuid not null
        primary key
        unique references users,
    theme int references themes
);