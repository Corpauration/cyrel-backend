alter table courses_groups
    add constraint courses_groups_pk
        unique (id, ref);

alter table professors
    add constraint professors_pk
        unique (id);

alter table rooms_courses
    add constraint rooms_courses_pk
        unique (id, ref);

alter table students
    add constraint students_pk
        unique (id);

alter table users_groups
    add constraint users_groups_pk
        unique (id, ref);

drop table tasks;
