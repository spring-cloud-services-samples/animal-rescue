create table animal (
  id bigint primary key auto_increment,
  name varchar(255) not null,
  rescue_date date,
  avatar_url varchar(255),
  description varchar(1000)
);

create table adoption_request (
  id bigint primary key auto_increment,
  animal bigint not null,
  adopter_name varchar(255) not null,
  email varchar(255) not null,
  notes varchar(1000)
);
