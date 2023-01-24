create table version
(
    id bool default false, -- Fake column
    version  text not null,
    platform text not null,
    url      text not null,
    constraint pk_version primary key (version, platform)
);
