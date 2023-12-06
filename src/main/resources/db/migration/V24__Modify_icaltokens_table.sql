drop table icaltokens;

create table icaltokens
(
    id      uuid primary key,
    email text not null unique
);
