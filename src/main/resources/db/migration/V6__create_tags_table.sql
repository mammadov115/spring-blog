create table tags(
    id bigserial primary key, 
    name varchar(255) not null unique
);

create table post_tags(
    post_id bigint not null references posts(id),
    tag_id bigint not null references tags(id), 
    primary key(post_id, tag_id)
)