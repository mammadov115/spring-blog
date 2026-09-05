import psycopg2
import random
import string

conn = psycopg2.connect(
    host="localhost",
    database="blog",
    user="postgres",
    password="admin"
)
cur = conn.cursor()

batch_size = 1000
total = 1_000_000

for batch_start in range(0, total, batch_size):
    values = []
    for i in range(batch_start, min(batch_start + batch_size, total)):
        tag = random.choice(["java", "spring", "docker", "postgresql"])
        slug = f"post-{i}-{''.join(random.choices(string.ascii_lowercase, k=6))}"
        values.append(f"('Post {i}', '{slug}', 'Body {i}', 'PUBLISHED', NOW(), NOW(), NOW())")
    
    cur.execute(f"INSERT INTO posts (title, slug, body, status, created, updated, publish) VALUES {','.join(values)}")
    conn.commit()
    print(f"{batch_start + batch_size}/{total} insert edildi")

cur.close()
conn.close()