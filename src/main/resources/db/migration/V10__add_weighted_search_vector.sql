-- temporary delete trigger and function if they exist
drop trigger if exists posts_search_vector_update on posts;
drop function if exists update_search_vector();

-- update existing posts with weighted search vector
update posts
set search_vector =
    setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(body, '')), 'B');

-- new trigger and function for future inserts/updates
create function update_search_vector() returns trigger as $$
begin
    new.search_vector :=
        setweight(to_tsvector('english', coalesce(new.title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(new.body, '')), 'B');
    return new;
end;
$$ language plpgsql;

create trigger posts_search_vector_update
before insert or update on posts
for each row execute function update_search_vector();
