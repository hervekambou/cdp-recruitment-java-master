-- Pre-cleaning
delete from event_bands;
delete from band_members;
delete from event;
delete from band;
delete from member;

-- Resetting identity sequences for IDs
alter table member alter column id restart with 1;
alter table band alter column id restart with 1;
alter table event alter column id restart with 1;

-- Members (including names containing 'Wa' to test the /search/Wa filter)
insert into member (id, name) values (1, 'Queen Anika Walsh');
insert into member (id, name) values (2, 'John Doe');
insert into member (id, name) values (3, 'Alice Wayne');

-- Groups
insert into band (id, name) values (1, 'Metallica');
insert into band (id, name) values (2, 'Muse');

-- Group ↔ Member relationships
insert into band_members (band_id, members_id) values (1, 1); -- Metallica ↔ Walsh
insert into band_members (band_id, members_id) values (1, 2); -- Metallica ↔ John Doe
insert into band_members (band_id, members_id) values (2, 3); -- Muse ↔ Alice Wayne

-- Events
insert into event (id, title, img_url, nb_stars, comment) values (1, 'GrasPop Metal Meeting', 'img/1000.jpeg', 4, 'Top');
insert into event (id, title, img_url, nb_stars, comment) values (2, 'Rock Werchter', 'img/2000.jpeg', 5, 'Legendary');

-- Event ↔ group relationships
insert into event_bands (event_id, bands_id) values (1, 1); -- GrasPop ↔ Metallica
insert into event_bands (event_id, bands_id) values (1, 2); -- GrasPop ↔ Muse
insert into event_bands (event_id, bands_id) values (2, 2); -- Rock Werchter ↔ Muse