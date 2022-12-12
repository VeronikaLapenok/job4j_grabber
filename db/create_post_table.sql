create table post if not exist (
    id serial primary key,
    name varchar(255),
    description text,
    link varchar(255) unique,
    created timestamp
);
