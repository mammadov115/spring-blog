drop index if exists idx_posts_search;

create index idx_posts_published_search
    on posts using gin(search_vector)
    where status = 'PUBLISHED';
