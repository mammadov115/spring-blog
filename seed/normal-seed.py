import psycopg2
from psycopg2.extras import execute_batch
from faker import Faker
import random
from datetime import datetime, timedelta
import hashlib
from tqdm import tqdm

fake = Faker()

conn = psycopg2.connect(
    host="localhost",
    port=5432,
    dbname="blog",
    user="postgres",
    password="admin"
)
cur = conn.cursor()

BATCH_SIZE = 10_000

def batch_insert_with_progress(cur, query, data, label):
    total = len(data)
    with tqdm(total=total, desc=label, unit="rows") as pbar:
        for i in range(0, total, BATCH_SIZE):
            batch = data[i:i + BATCH_SIZE]
            execute_batch(cur, query, batch, page_size=BATCH_SIZE)
            pbar.update(len(batch))
    conn.commit()

#  USERS 
print("\n📦 Generating users...")
users = []
for _ in tqdm(range(100_000), desc="Generating users", unit="rows"):
    users.append((
        fake.unique.user_name(),
        fake.unique.email(),
        hashlib.sha256(fake.password().encode()).hexdigest()
    ))

batch_insert_with_progress(cur, """
    INSERT INTO users (username, email, password)
    VALUES (%s, %s, %s)
""", users, "Inserting users")

# POSTS 
cur.execute("SELECT id FROM users")
user_ids = [r[0] for r in cur.fetchall()]

statuses = ["draft", "published", "archived"]

print("\n📦 Generating posts...")
posts = []
for _ in tqdm(range(500_000), desc="Generating posts", unit="rows"):
    title = fake.sentence(nb_words=6).rstrip(".")
    slug = title.lower().replace(" ", "-") + "-" + fake.uuid4()[:8]
    created = fake.date_time_between(start_date="-3y", end_date="now")
    updated = created + timedelta(days=random.randint(0, 30))
    publish = updated if random.random() > 0.3 else None
    posts.append((
        title,
        fake.text(max_nb_chars=2000),
        publish,
        created,
        updated,
        random.choice(statuses),
        random.choice(user_ids),
        slug
    ))

batch_insert_with_progress(cur, """
    INSERT INTO posts (title, body, publish, created, updated, status, author_id, slug)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
""", posts, "Inserting posts")

#  TAGS 
print("\n📦 Generating tags...")
tag_names = list(set([fake.word() for _ in range(500)]))
tags = [(t,) for t in tag_names]

batch_insert_with_progress(cur, """
    INSERT INTO tags (name) VALUES (%s)
    ON CONFLICT (name) DO NOTHING
""", tags, "Inserting tags")

cur.execute("SELECT id FROM tags")
tag_ids = [r[0] for r in cur.fetchall()]

#  POST_TAGS 
print("\n📦 Generating post_tags...")
cur.execute("SELECT id FROM posts")
post_ids = [r[0] for r in cur.fetchall()]

post_tags = set()
for post_id in tqdm(post_ids, desc="Generating post_tags", unit="posts"):
    for tag_id in random.sample(tag_ids, k=random.randint(1, 5)):
        post_tags.add((post_id, tag_id))

batch_insert_with_progress(cur, """
    INSERT INTO post_tags (post_id, tag_id)
    VALUES (%s, %s)
    ON CONFLICT DO NOTHING
""", list(post_tags), "Inserting post_tags")

#  COMMENTS 
print("\n📦 Generating comments...")
comments = []
for _ in tqdm(range(400_000), desc="Generating comments", unit="rows"):
    created = fake.date_time_between(start_date="-3y", end_date="now")
    comments.append((
        random.choice(post_ids),
        fake.name(),
        fake.email(),
        fake.text(max_nb_chars=500),
        created
    ))

batch_insert_with_progress(cur, """
    INSERT INTO comments (post_id, name, email, body, created)
    VALUES (%s, %s, %s, %s, %s)
""", comments, "Inserting comments")

cur.close()
conn.close()
print("\n✅ Done! Total ~1M rows inserted.")