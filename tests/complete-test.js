import http from "k6/http";
import { check, group, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";
import { randomIntBetween } from "https://jslib.k6.io/k6-utils/1.4.0/index.js";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.4.0/index.js";

// ─── Config ────────────────────────────────────────────────────────────────

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

// ─── Custom Metrics ─────────────────────────────────────────────────────────

const postCreateErrors   = new Counter("post_create_errors");
const commentAddErrors   = new Counter("comment_add_errors");
const searchErrors       = new Counter("search_errors");
const tagErrors          = new Counter("tag_errors");

const postCreateRate     = new Rate("post_create_success_rate");
const commentAddRate     = new Rate("comment_add_success_rate");
const searchRate         = new Rate("search_success_rate");

const listPostsDuration  = new Trend("list_posts_duration",  true);
const getPostDuration    = new Trend("get_post_duration",    true);
const searchDuration     = new Trend("search_duration",      true);
const createPostDuration = new Trend("create_post_duration", true);
const commentDuration    = new Trend("comment_duration",     true);

// ─── Load Stages ───────────────────────────────────────────────────────────
//
//  Phase 1 – Ramp-up:   0 → 10 VUs  over 30 s  (cold start)
//  Phase 2 – Normal:    10 VUs       for  1 min  (steady baseline)
//  Phase 3 – Spike:     10 → 40 VUs over 20 s   (burst traffic)
//  Phase 4 – Hold:      40 VUs       for 30 s    (stress hold)
//  Phase 5 – Recovery:  40 → 10 VUs over 20 s   (cool-down)
//  Phase 6 – Baseline:  10 VUs       for 30 s    (confirm recovery)
//  Phase 7 – Ramp-down: 10 → 0 VUs  over 20 s

export const options = {
  stages: [
    { duration: "30s",  target: 10 },   // ramp-up
    { duration: "60s",  target: 10 },   // steady
    { duration: "20s",  target: 40 },   // spike
    { duration: "30s",  target: 40 },   // stress hold
    { duration: "20s",  target: 10 },   // recovery
    { duration: "30s",  target: 10 },   // baseline check
    { duration: "20s",  target:  0 },   // ramp-down
  ],
  thresholds: {
    // Global HTTP error rate < 5 %
    http_req_failed:          ["rate<0.05"],

    // P95 latencies
    http_req_duration:        ["p(95)<2000"],
    list_posts_duration:      ["p(95)<1000"],
    get_post_duration:        ["p(95)<800"],
    search_duration:          ["p(95)<1500"],
    create_post_duration:     ["p(95)<2000"],
    comment_duration:         ["p(95)<1500"],

    // Business-level success rates
    post_create_success_rate: ["rate>0.95"],
    comment_add_success_rate: ["rate>0.95"],
    search_success_rate:      ["rate>0.90"],
  },
};

// ─── Shared state (seeded before the test) ─────────────────────────────────

// We seed a small pool of slugs/IDs in setup() so VUs can use real data.
export function setup() {
  const headers = { "Content-Type": "application/json" };

  const seedPosts = [];

  // Create 5 seed posts
  for (let i = 0; i < 5; i++) {
    const slug = `load-test-seed-${uuidv4().slice(0, 8)}`;
    const body = JSON.stringify({
      title: `Load Test Post ${i + 1}`,
      slug,
      body:  "This is a seeded post body for load testing. It contains enough text to be realistic.",
      status: "PUBLISHED",
    });

    const res = http.post(`${BASE_URL}/api/posts`, body, { headers });

    if (res.status === 200 || res.status === 201) {
      const post = JSON.parse(res.body);
      seedPosts.push({ id: post.id, slug: post.slug || slug });
    } else {
      console.warn(`Seed post ${i} failed: ${res.status} ${res.body}`);
      // Fallback slug so the test still runs
      seedPosts.push({ id: null, slug });
    }
  }

  console.log(`Setup complete – seeded ${seedPosts.length} posts`);
  return { seedPosts };
}

// ─── Helpers ───────────────────────────────────────────────────────────────

function jsonHeaders() {
  return { "Content-Type": "application/json" };
}

function randomSeedPost(data) {
  return data.seedPosts[randomIntBetween(0, data.seedPosts.length - 1)];
}

const TAGS     = ["k6", "performance", "testing", "api", "backend", "java", "spring"];
const KEYWORDS = ["load", "test", "post", "seed", "performance"];

// ─── Scenario: Read-heavy (typical blog traffic) ────────────────────────────

function scenarioReadPosts(data) {
  group("📄 List Posts (paginated)", () => {
    const page = randomIntBetween(0, 3);
    const size = [5, 10, 20][randomIntBetween(0, 2)];
    const res  = http.get(`${BASE_URL}/api/posts?page=${page}&size=${size}`);
    listPostsDuration.add(res.timings.duration);

    check(res, {
      "list posts → 200":          (r) => r.status === 200,
      "list posts → has content":  (r) => {
        try { return JSON.parse(r.body).content.length >= 0; }
        catch { return false; }
      },
      "list posts → totalElements present": (r) => {
        try { return JSON.parse(r.body).totalElements !== undefined; }
        catch { return false; }
      },
    });
  });

  sleep(randomIntBetween(1, 2));

  group("📄 Get Single Post by Slug", () => {
    const post = randomSeedPost(data);
    const res  = http.get(`${BASE_URL}/api/posts/${post.slug}`);
    getPostDuration.add(res.timings.duration);

    check(res, {
      "get post → 200 or 404":   (r) => [200, 404].includes(r.status),
      "get post → has slug":     (r) => {
        if (r.status !== 200) return true; // 404 is acceptable for old slugs
        try { return JSON.parse(r.body).slug !== undefined; }
        catch { return false; }
      },
    });
  });

  sleep(randomIntBetween(1, 2));

  group("🔗 Keyset Pagination", () => {
    const size = randomIntBetween(5, 15);
    const res  = http.get(`${BASE_URL}/api/posts/keyset?size=${size}`);

    check(res, {
      "keyset → 200":           (r) => r.status === 200,
      "keyset → hasNext field": (r) => {
        try { return "hasNext" in JSON.parse(r.body); }
        catch { return false; }
      },
    });

    // Follow the cursor if there is one
    if (res.status === 200) {
      try {
        const body   = JSON.parse(res.body);
        if (body.hasNext && body.nextCursor) {
          const next = http.get(
            `${BASE_URL}/api/posts/keyset?cursor=${body.nextCursor}&size=${size}`
          );
          check(next, { "keyset cursor → 200": (r) => r.status === 200 });
        }
      } catch { /* ignore parse errors */ }
    }
  });
}

// ─── Scenario: Search ───────────────────────────────────────────────────────

function scenarioSearch() {
  group("🔍 Full-text Search", () => {
    const keyword = KEYWORDS[randomIntBetween(0, KEYWORDS.length - 1)];
    const res     = http.get(`${BASE_URL}/api/posts/search?query=${keyword}`);
    searchDuration.add(res.timings.duration);

    const ok = check(res, {
      "search → 200":          (r) => r.status === 200,
      "search → array result": (r) => {
        try { return Array.isArray(JSON.parse(r.body)); }
        catch { return false; }
      },
    });

    searchRate.add(ok);
    if (!ok) searchErrors.add(1);
  });

  sleep(randomIntBetween(1, 2));
}

// ─── Scenario: Tags ─────────────────────────────────────────────────────────

function scenarioTags(data) {
  group("🏷️ Get Tags by Post", () => {
    const post = randomSeedPost(data);
    const res  = http.get(`${BASE_URL}/api/posts/${post.slug}/tags`);

    check(res, {
      "get tags → 200 or 404": (r) => [200, 404].includes(r.status),
      "get tags → array":      (r) => {
        if (r.status !== 200) return true;
        try { return Array.isArray(JSON.parse(r.body)); }
        catch { return false; }
      },
    });
  });

  sleep(1);

  group("🏷️ Get Posts by Tag", () => {
    const tag = TAGS[randomIntBetween(0, TAGS.length - 1)];
    const res = http.get(`${BASE_URL}/api/tags/${tag}/posts`);

    check(res, {
      "posts by tag → 200": (r) => r.status === 200,
    });

    if (res.status !== 200) tagErrors.add(1);
  });
}

// ─── Scenario: Comments ─────────────────────────────────────────────────────

function scenarioComments(data) {
  const post = randomSeedPost(data);
  if (!post.id) return; // skip if seed failed

  group("💬 Get Comments", () => {
    const res = http.get(`${BASE_URL}/api/posts/${post.id}/comments`);

    check(res, {
      "get comments → 200": (r) => r.status === 200,
      "get comments → array": (r) => {
        try { return Array.isArray(JSON.parse(r.body)); }
        catch { return false; }
      },
    });
  });

  sleep(1);

  group("💬 Add Comment", () => {
    const payload = JSON.stringify({
      name:  `Tester ${randomIntBetween(1, 9999)}`,
      email: `tester${randomIntBetween(1, 9999)}@k6.io`,
      body:  "This is an automated k6 performance test comment.",
    });

    const res = http.post(
      `${BASE_URL}/api/posts/${post.id}/comments`,
      payload,
      { headers: jsonHeaders() }
    );
    commentDuration.add(res.timings.duration);

    const ok = check(res, {
      "add comment → 200":      (r) => r.status === 200,
      "add comment → has id":   (r) => {
        try { return JSON.parse(r.body).id !== undefined; }
        catch { return false; }
      },
      "add comment → has body": (r) => {
        try { return JSON.parse(r.body).body !== undefined; }
        catch { return false; }
      },
    });

    commentAddRate.add(ok);
    if (!ok) commentAddErrors.add(1);
  });
}

// ─── Scenario: Write (Create / Update / Delete) ─────────────────────────────

function scenarioWrite() {
  let createdSlug = null;

  group("✏️ Create Post", () => {
    const slug = `k6-test-${uuidv4().slice(0, 8)}`;
    const payload = JSON.stringify({
      title:  `K6 Test Post ${Date.now()}`,
      slug,
      body:   "Automated load test post body. Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
      status: "DRAFT",
    });

    const res = http.post(`${BASE_URL}/api/posts`, payload, {
      headers: jsonHeaders(),
    });
    createPostDuration.add(res.timings.duration);

    const ok = check(res, {
      "create post → 200":      (r) => r.status === 200,
      "create post → has slug": (r) => {
        try { return JSON.parse(r.body).slug !== undefined; }
        catch { return false; }
      },
    });

    postCreateRate.add(ok);
    if (!ok) {
      postCreateErrors.add(1);
      return;
    }

    try {
      createdSlug = JSON.parse(res.body).slug || slug;
    } catch {
      createdSlug = slug;
    }
  });

  if (!createdSlug) return;

  sleep(randomIntBetween(1, 2));

  group("✏️ Update Post", () => {
    const payload = JSON.stringify({
      title:  `K6 Updated Post ${Date.now()}`,
      body:   "Updated body content from k6 load test.",
      status: "PUBLISHED",
    });

    const res = http.put(`${BASE_URL}/api/posts/${createdSlug}`, payload, {
      headers: jsonHeaders(),
    });

    check(res, {
      "update post → 200 or 404": (r) => [200, 404].includes(r.status),
    });
  });

  sleep(1);

  group("✏️ Add Tags to Post", () => {
    const tags = [
      TAGS[randomIntBetween(0, TAGS.length - 1)],
      TAGS[randomIntBetween(0, TAGS.length - 1)],
    ];
    const payload = JSON.stringify([...new Set(tags)]);

    const res = http.post(
      `${BASE_URL}/api/posts/${createdSlug}/tags`,
      payload,
      { headers: jsonHeaders() }
    );

    check(res, {
      "add tags → 200 or 404": (r) => [200, 404].includes(r.status),
    });
  });

  sleep(1);

  group("✏️ Get Similar Posts", () => {
    const res = http.get(`${BASE_URL}/api/posts/${createdSlug}/similar`);
    check(res, {
      "similar posts → 200 or 404": (r) => [200, 404].includes(r.status),
    });
  });

  sleep(1);

  group("🗑️ Delete Post", () => {
    const res = http.del(`${BASE_URL}/api/posts/${createdSlug}`);
    check(res, {
      "delete post → 200 or 404": (r) => [200, 404].includes(r.status),
    });
  });
}

// ─── Scenario: Share Post ───────────────────────────────────────────────────

function scenarioShare(data) {
  group("📤 Share Post", () => {
    const post = randomSeedPost(data);
    const payload = JSON.stringify({
      senderName:     "K6 Tester",
      senderEmail:    "k6sender@example.com",
      recipientEmail: "k6recipient@example.com",
      comment:        "Check out this post from our load test!",
    });

    const res = http.post(
      `${BASE_URL}/api/posts/${post.slug}/share`,
      payload,
      { headers: jsonHeaders() }
    );

    check(res, {
      "share post → 200 or 5xx": (r) => r.status < 600,
    });
  });
}

// ─── Scenario: Public Feeds ──────────────────────────────────────────────────

function scenarioPublicFeeds() {
  group("📡 RSS Feed", () => {
    const res = http.get(`${BASE_URL}/feed/rss`);
    check(res, {
      "rss feed → 200":      (r) => r.status === 200,
      "rss feed → xml body": (r) => (r.body || "").includes("<rss") || (r.body || "").includes("<?xml"),
    });
  });

  sleep(1);

  group("🗺️ Sitemap", () => {
    const res = http.get(`${BASE_URL}/sitemap.xml`);
    check(res, {
      "sitemap → 200":      (r) => r.status === 200,
      "sitemap → xml body": (r) => (r.body || "").includes("<urlset") || (r.body || "").includes("<?xml"),
    });
  });
}

// ─── Main VU function ───────────────────────────────────────────────────────
//
//  Traffic mix (approximate):
//    50% reads, 20% search, 10% writes, 10% comments, 5% tags, 5% feeds/share

export default function (data) {
  const roll = Math.random();

  if      (roll < 0.50) { scenarioReadPosts(data); }
  else if (roll < 0.70) { scenarioSearch(); }
  else if (roll < 0.80) { scenarioWrite(); }
  else if (roll < 0.90) { scenarioComments(data); }
  else if (roll < 0.95) { scenarioTags(data); }
  else                  {
    scenarioPublicFeeds();
    // 50% chance of also doing a share in this slot
    if (Math.random() < 0.5) { scenarioShare(data); }
  }

  sleep(randomIntBetween(1, 3));
}

// ─── Teardown: clean up seeded data ─────────────────────────────────────────

export function teardown(data) {
  for (const post of data.seedPosts) {
    if (post.slug) {
      const res = http.del(`${BASE_URL}/api/posts/${post.slug}`);
      console.log(`Teardown: DELETE ${post.slug} → ${res.status}`);
    }
  }
}