create table students
(
    id         uuid references users,
    student_id int4 not null
);

create table professors
(
    id uuid references users
);
