-- First remove the join tables to respect the FK constraints.
delete from event_bands;
delete from band_members;

-- Then the main tables
delete from event;
delete from band;
delete from member;

-- Reset identities for stable IDs between tests
alter table member alter column id restart with 1;
alter table band alter column id restart with 1;
alter table event alter column id restart with 1;