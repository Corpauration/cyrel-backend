create table courses_alerts
(
    id      text      not null
        constraint courses_alerts_courses_id_fk
            references courses,
    "group" integer   not null
        constraint courses_alerts_groups_id_fk
            references groups,
    time    timestamp not null,
    event   integer   not null,
    constraint courses_alerts_pk
        primary key (id, "group", time)
);

