drop table if exists rooms cascade;

create table rooms
(
    id        text not null
        primary key
        unique,
    name      text not null,
    capacity  int  not null,
    computers bool not null
);

drop table if exists rooms_availabilities;

create table rooms_courses
(
    id  text references rooms,
    ref text references courses
);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU A001 AMPHITHÉÂTRE 202p', 'PAU A001', 202, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E101 LABO DE LANGUES 49p', 'PAU E101', 30, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E102 SALLES POLYVALENTE (TD ET INFO) 40p', 'PAU E102', 40, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E104 SALLES POLYVALENTE (TD ET INFO) 32p', 'PAU E104', 30, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E105 SALLES INFORMATIQUE 40p', 'PAU E105', 40, true);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E106 SALLES INFORMATIQUE 40p', 'PAU E106', 40, true);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E107 SALLES DE TD 32p', 'PAU E107', 30, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E108 SALLES INFORMATIQUE 40p', 'PAU E108', 40, true);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E109 SALLES POLYVALENTE (TD ET INFO) 41p', 'PAU E109', 40, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E110 SALLES POLYVALENTE (TD ET INFO) 32p', 'PAU E110', 35, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E201 SALLES POLYVALENTE (TD ET INFO) 30p', 'PAU E201', 30, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E209 SALLES POLYVALENTE (TD ET INFO) 40p', 'PAU E209', 40, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E211 SALLES POLYVALENTE (TD ET INFO) 60p', 'PAU E211', 60, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E212 SALLES POLYVALENTE (TD ET INFO) 40p', 'PAU E212', 40, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E213 SALLES POLYVALENTE (TD ET INFO) 18p', 'PAU E213', 30, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E214 SALLES POLYVALENTE (TD ET INFO) 40p', 'PAU E214', 40, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E215 SALLES POLYVALENTE (TD ET INFO) 40p', 'PAU E215', 40, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E216 SALLES DE RÉUNION 18p', 'PAU E216', 20, false);

INSERT INTO rooms (id, name, capacity, computers)
VALUES ('PAU E218 SALLES POLYVALENTE (TD ET INFO) 80p', 'PAU E218', 80, false);
