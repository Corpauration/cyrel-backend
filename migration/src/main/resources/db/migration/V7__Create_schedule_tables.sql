create table courses
(
    id       text          not null
        primary key
        unique,
    start    date          not null,
    "end"    date,
    category int default 0 not null,
    subject  text,
    teachers text          not null,
    rooms    text          not null
);

create table courses_groups
(
    id text references courses,
    ref int references groups
);