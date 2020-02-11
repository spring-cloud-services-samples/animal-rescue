insert into users (id,username,password,email,verified) values (uuid(), 'test_user', '$2a$10$tHEK0BPseuBLCMTfFKbatOsOyvg5GrL98w1xzF5n2JZEonOGLfcH.', 'test_user', 1);
insert into user_info (user_id, info) values ((select id from users where email = 'test_user'), '{"authorities": [ "adoption.request" ] }');

insert into users (id,username,password,email,verified) values (uuid(), 'user_without_authorities', '$2a$10$tHEK0BPseuBLCMTfFKbatOsOyvg5GrL98w1xzF5n2JZEonOGLfcH.', 'user_without_scope', 1);
insert into user_info (user_id, info) values ((select id from users where email = 'user_without_authorities'), '{"authorities": [] }');
