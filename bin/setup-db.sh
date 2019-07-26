#!/usr/bin/env bash

createdb globardb

psql globardb -a  <<__END
create user globar_role password 'j3mc';
__END

psql -Uglobar_role globardb -a <<__END
drop table if exists vendor_calendar;
drop table if exists vendor_rating;
drop table if exists Images;
drop table if exists Bookings;
drop table if exists Vendors;
drop table if exists Services;
drop table if exists Users;

CREATE OR REPLACE FUNCTION maintain_updated_at()
RETURNS TRIGGER AS \$\$
BEGIN
   NEW.updated_at = now();
   RETURN NEW;
END;
\$\$ language 'plpgsql';

create table Users (
  user_id int generated by default as identity primary key,
  name_first text,
  name_last text,
  name text,
  email text not null,
  email_verified boolean,
  is_vendor boolean default false not null,
  addr_str_num text,
  addr_str_name text,
  addr_city text,
  addr_state text,
  addr_postal text,
  phone text,
  locale text,
  sub text not null,
  avatar text,
  created_at timestamptz not null default current_timestamp,
  updated_at timestamptz not null default current_timestamp);
create trigger user_updated_at before update
on Users for each row execute procedure
maintain_updated_at();

/* this index ensures that email addresses are unique in the system */
create unique index idx_user_email on Users(email);
create unique index idx_user_sub on Users(sub);

create table Vendors (
  vendor_id int generated by default as identity primary key,
  user_id int not null references Users(user_id),
  summary text,
  radius int,
  profile_pic text,
  template text,
  created_at timestamptz not null default current_timestamp,
  updated_at timestamptz not null default current_timestamp);
create trigger vendor_updated_at before update
on Vendors for each row execute procedure
maintain_updated_at();

create table Services (
  service_id int generated by default as identity primary key,
  vendor_id int,
  s_name text,
  s_description text,
  s_type text,
  s_price int,
  s_duration int,
  created_at timestamptz not null default current_timestamp,
  updated_at timestamptz not null default current_timestamp);
create trigger service_updated_at before update
on Services for each row execute procedure
maintain_updated_at();

create table vendor_rating (
  vendor_id int references Vendors(vendor_id),
  user_id int references Users(user_id),
  rating integer not null,
  created_at timestamptz not null default current_timestamp,
  updated_at timestamptz not null default current_timestamp
);
create trigger vendor_rating_updated_at before update
on vendor_rating for each row execute procedure
maintain_updated_at();

/* this is required to support the upsert operation
   and constrains one review per user per vendor */
create unique index idx_vid_uid on vendor_rating (vendor_id, user_id);

/* each row of this table represents the available time and the booked 
   time in the calendar for a vendor */
create table vendor_calendar (
  vendor_id int references Vendors(vendor_id),
  date varchar(10),
  available_edn text,
  booked_edn text,
  created_at timestamptz not null default current_timestamp,
  updated_at timestamptz not null default current_timestamp
);
create trigger vendor_calendar_updated_at before update
on vendor_calendar for each row execute procedure
maintain_updated_at();

/* each row of this table represents a booking*/
create table Bookings (
  booking_id int generated by default as identity primary key,
  vendor_id int not null references Vendors(vendor_id),
  user_id int not null references Users(user_id),
  start_time int not null,
  end_time int not null,
  date date not null,
  service text,
  cancelled boolean,
  created_at timestamptz not null default current_timestamp,
  updated_at timestamptz not null default current_timestamp
);
create trigger bookings_updated_at before update
on Bookings for each row execute procedure
maintain_updated_at();

/* each row of this table represents an image */
create table Images (
  vendor_id int not null references Vendors(vendor_id),
  filename text not null,
  metadata text,
  description text,
  service_id int not null references Services(service_id),
  created_at timestamptz not null default current_timestamp,
  updated_at timestamptz not null default current_timestamp
);
create trigger images_updated_at before update
on Images for each row execute procedure
maintain_updated_at();

insert into Users (user_id, addr_city, name_first, name_last, email, sub) values
  (37,   'vancouver', 'mr. john', 'dough', 'curious@gmail.com', 'sub1'),
  (234,  'VanCouVer', 'Wishful', 'Wanda',   'wanda@gmail.com', 'sub2'),
  (235,  'Vancouver', 'Helen', 'Hairspray', 'helen@gmail.com', 'sub3'),
  (236,  'Surrey', 'Jackie', 'Jones',    'jackie@gmail.com', 'sub4'),
  (237,  'vancouver', 'Tony', 'Toenails',   'tony@gmail.com', 'sub5'),
  (1410, 'Burnaby', 'alex', 'attenborough', 'bleeding@hotmail.com', 'sub6'),
  (2812, 'Vancouver', 'nancy', 'drew', 'missyo@abc.com', 'sub7');
alter table Users alter column user_id restart with 2900;

insert into Vendors (vendor_id, user_id, summary, profile_pic, template) values
  (1234, 234, 'We make sure your toes don''t look Dumb and Dumber', 'wanda-profile.jpg', '{:Thursday [[360 420]]}'),
  (1235, 235, 'In business since yesterday, we make sure you don''t die from fumes', 'helen-profile.jpg', '{:Wednesday [[700 800]]}'),
  (1236, 236, 'Your fast track to a smooth butt', 'jackie-profile.jpg', '{}'),
  (1237, 237, 'We make sure your face is tastefully decorated for all occasions', 'tony-profile.jpg', '{:Saturday [[0 60]] :Sunday [[0 60]]}');
alter table Vendors alter column vendor_id restart with 1300;

insert into vendor_calendar (vendor_id, date, available_edn, booked_edn) values
  (1234, '2019-07-18', '[[0 299] [1000 1439]]', '[[60 119]]'),
  (1235, '2019-07-18', '[[0 599]]', '[[0 59] [540 599]]'),
  (1236, '2019-07-18', '[[0 1439]]', '[[300 359] [420 599]]'),
  (1237, '2019-07-18', '[[600 1199] [1300 1329]]', '[[600 659] [720 779]]');

insert into vendor_rating (vendor_id, user_id, rating) values
  (1234, 37, 3),
  (1234, 1410, 5),
  (1236, 1410, 4),
  (1237, 1410, 4),
  (1237, 2812, 4),
  (1237, 37, 5);

insert into Services (vendor_id, s_name, s_description, s_type, s_price, s_duration) values
  (1234, 'french manicure', 'covers paint and laquer for 10 fingers', 'nails', 2000, 30),
  (1234, 'spanish manicure', 'involves hot sauce and salsa', 'nails', 3000, 45),
  (1235, 'nail trim and polish', 'your pick of 5 colors plus a clear coat', 'nails', 4000, 40),
  (1235, 'blow', 'refurbish your hair to salon style', 'hair', 3000, 30),
  (1235, 'trim', 'will give your hair a quick trim', 'hair', 8000, 60),
  (1235, 'color', 'will pick a color for your hair and give you the full stinky treatment', 'hair', 10000, 90),
  (1236, 'file and polish', 'will smooth and polish your fingernails', 'nails', 6000, 60),
  (1237, 'back massage', 'an intense 30 minute upper back massage', 'massage', 8000, 30),
  (1237, 'full massage', 'will massage your face, back and legs', 'massage', 12000, 60);
__END
