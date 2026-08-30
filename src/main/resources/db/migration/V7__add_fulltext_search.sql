alter table posts
add column search_vector tsvector;
update posts
set search_vector = to_tsvector('english', title || ' ' || body);
create index idx_posts_search on posts using gin(search_vector);
create function update_search_vector() returns trigger as $$
begin new.search_vector := to_tsvector('english', new.title || ' ' || new.body);
return new;
end;
$$ language plpgsql;
create trigger posts_search_vector_update before
insert
    or
update on posts for each row execute function update_search_vector();