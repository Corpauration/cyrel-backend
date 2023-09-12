create table icaltokens
(
    id      text primary key,
    "user"  uuid not null references users,
    private text not null,
    public  text not null
);
