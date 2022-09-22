INSERT INTO groups (id, name, referent, parent, private)
VALUES (-1, 'admin', null, null, true)
ON CONFLICT DO NOTHING;

INSERT INTO groups (id, name, referent, parent, private)
VALUES (-2, 'responsable devoir', null, null, true)
ON CONFLICT DO NOTHING;

INSERT INTO groups (id, name, referent, parent, private)
VALUES (-3, 'délégué', null, null, true)
ON CONFLICT DO NOTHING;