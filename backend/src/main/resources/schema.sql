create table animal (
  id bigint primary key auto_increment,
  name varchar(36) not null,
  rescue_date date,
  avatar_url varchar(255),
  description varchar(1000)
);

create table adoption_request (
  id bigint primary key auto_increment,
  animal varchar(36) not null,
  adopter_name varchar(36) not null,
  notes varchar(1000)
);
