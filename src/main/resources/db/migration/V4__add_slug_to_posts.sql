ALTER TABLE posts
ADD COLUMN slug VARCHAR(255);
UPDATE posts
SET slug = id::text
WHERE slug IS NULL;
ALTER TABLE posts
ALTER COLUMN slug
SET NOT NULL;
CREATE UNIQUE INDEX idx_posts_slug ON posts(slug);