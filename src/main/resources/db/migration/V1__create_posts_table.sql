create table posts (
    id bigserial primary key,
    title varchar(255) not null,
    body text not null,
    publish timestamp,
    created timestamp not null,
    updated timestamp not null,
    status varchar(255) not null,
    author_id bigint
);