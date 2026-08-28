create table comments (
    id bigserial primary key,
    post_id bigint not null references posts(id),
    name varchar(255) not null,
    email varchar(255) not null,
    body text,
    created timestamp not null
)