alter table posts
add constraint fk_posts_author foreign key (author_id) references users(id);