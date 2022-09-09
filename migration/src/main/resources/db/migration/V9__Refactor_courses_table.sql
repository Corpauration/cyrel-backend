alter table courses
    alter column start type timestamp using start::timestamp;

alter table courses
    alter column "end" type timestamp using "end"::timestamp;

