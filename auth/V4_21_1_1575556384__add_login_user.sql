-- update users
-- set id = "test_user"
-- where username = "test";
insert into users (id,username,password,email,verified) values ('mysterious_adopter', 'mysterious_adopter', '$2a$10$tHEK0BPseuBLCMTfFKbatOsOyvg5GrL98w1xzF5n2JZEonOGLfcH.', 'mysterious_adopter@example.com', 1);
insert into user_info (user_id, info) values ('mysterious_adopter', '{"user_attributes": {}, "roles": [] }');
