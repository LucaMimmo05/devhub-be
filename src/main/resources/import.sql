-- This file allow to write SQL commands that will be emitted in test and dev.
-- The commands are commented as their support depends of the database
-- insert into myentity (id, field) values(1, 'field-1');
-- insert into myentity (id, field) values(2, 'field-2');
-- insert into myentity (id, field) values(3, 'field-3');
-- alter sequence myentity_seq restart with 4;

-- Manual fix for missing username column in user_profiles
-- ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS username VARCHAR(50);
-- ALTER TABLE user_profiles ADD CONSTRAINT user_profiles_username_unique UNIQUE (username);