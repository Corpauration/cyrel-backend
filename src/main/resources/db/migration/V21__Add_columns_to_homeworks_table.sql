alter table homeworks
    add last_modified_by uuid references users,
    add last_modified_at timestamp,
    add created_at       timestamp;
