/**
 * k6 test — GET /api/posts/{slug}
 *
 * Run:
 *   k6 run get-post-by-slug.k6.js
 *   k6 run -e BASE_URL=http://your-host:8080 -e KNOWN_SLUG=my-real-post get-post-by-slug.k6.js
 */

import http from "k6/http";
import { check, group, sleep } from "k6";
import { Trend, Rate, Counter } from "k6/metrics";

// ─── Config ────────────────────────────────────────────────────────────────

const BASE_URL   = __ENV.BASE_URL   || "http://localhost:8080";
const KNOWN_SLUG = __ENV.KNOWN_SLUG || null; // seeded in setup() if not provided

// ─── Custom metrics ────────────────────────────────────────────────────────

const validSlugDuration   = new Trend("duration_valid_slug",   true);
const invalidSlugDuration = new Trend("duration_invalid_slug", true);
const schemaErrors        = new Counter("schema_errors");
const notFoundRate        = new Rate("correct_404_rate");

// ─── Helpers ───────────────────────────────────────────────────────────────

function url(slug) {
  return `${BASE_URL}/api/posts/${slug}`;
}

/** Returns true if the response body matches the PostResponse schema */
function validSchema(r) {
  try {
    const b = JSON.parse(r.body);
    return (
      typeof b.id         === "number"  &&
      typeof b.title      === "string"  &&
      typeof b.slug       === "string"  &&
      typeof b.body       === "string"  &&
      typeof b.status     === "string"  &&
      Array.isArray(b.tags)
    );
  } catch {
    return false;
  }
}

/** Returns true if publish field is a valid ISO-8601 datetime string */
function validPublishDate(r) {
  try {
    const val = JSON.parse(r.body).publish;
    if (val === null || val === undefined) return true; // nullable is OK
    return !isNaN(Date.parse(val));
  } catch {
    return false;
  }
}

// ─── Setup: create one real post so we have a known slug ──────────────────

export function setup() {
  if (KNOWN_SLUG) {
    console.log(`Using provided KNOWN_SLUG: ${KNOWN_SLUG}`);
    return { slug: KNOWN_SLUG, createdBySetup: false };
  }

  const slug = `k6-slug-test-${Date.now()}`;
  const res = http.post(
    `${BASE_URL}/api/posts`,
    JSON.stringify({ title: "k6 slug test post", slug, body: "body content", status: "PUBLISHED" }),
    { headers: { "Content-Type": "application/json" } }
  );

  if (res.status === 200 || res.status === 201) {
    const actual = JSON.parse(res.body).slug || slug;
    console.log(`Setup: created post with slug="${actual}"`);
    return { slug: actual, createdBySetup: true };
  }

  console.warn(`Setup: POST failed (${res.status}), falling back to slug="${slug}"`);
  return { slug, createdBySetup: false };
}

// ─── Options ───────────────────────────────────────────────────────────────

export const options = {
  scenarios: {
    // Scenario A – functional / edge-case checks (1 VU, runs once)
    functional: {
      executor:    "shared-iterations",
      vus:         1,
      iterations:  1,
      maxDuration: "30s",
      exec:        "functionalScenario",
    },

    // Scenario B – performance check on the happy path (ramp up to 20 VUs)
    performance: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "15s", target: 10 },
        { duration: "30s", target: 20 },
        { duration: "15s", target:  0 },
      ],
      startTime: "35s", // starts after functional finishes
      exec:      "performanceScenario",
    },
  },

  thresholds: {
    // Overall HTTP failure rate
    http_req_failed:       ["rate<0.01"],

    // Valid slug must be fast
    duration_valid_slug:   ["p(95)<500", "p(99)<1000"],

    // Invalid slugs should also be fast (no expensive lookups)
    duration_invalid_slug: ["p(95)<400"],

    // Every 404 we expect must actually be a 404
    correct_404_rate:      ["rate>0.99"],

    // No schema violations on 200 responses
    schema_errors:         ["count==0"],
  },
};

// ═══════════════════════════════════════════════════════════════════════════
// SCENARIO A — Functional & edge-case tests
// ═══════════════════════════════════════════════════════════════════════════

export function functionalScenario(data) {
  const knownSlug = data.slug;

  // ── 1. Happy path ────────────────────────────────────────────────────────
  group("1. Happy path — valid existing slug", () => {
    const res = http.get(url(knownSlug));
    validSlugDuration.add(res.timings.duration);

    const schemaOk = validSchema(res);
    if (!schemaOk) schemaErrors.add(1);

    check(res, {
      "status 200":                  (r) => r.status === 200,
      "content-type is json":        (r) => (r.headers["Content-Type"] || "").includes("application/json"),
      "schema valid":                (_) => schemaOk,
      "slug matches request":        (r) => { try { return JSON.parse(r.body).slug === knownSlug; } catch { return false; } },
      "publish is valid date":       (r) => validPublishDate(r),
      "tags is array":               (r) => { try { return Array.isArray(JSON.parse(r.body).tags); } catch { return false; } },
      "id is positive integer":      (r) => { try { return Number.isInteger(JSON.parse(r.body).id) && JSON.parse(r.body).id > 0; } catch { return false; } },
      "status field is non-empty":   (r) => { try { return JSON.parse(r.body).status.length > 0; } catch { return false; } },
    });
  });

  sleep(0.5);

  // ── 2. Non-existent slug ─────────────────────────────────────────────────
  group("2. Non-existent slug → expect 404", () => {
    const res = http.get(url("this-post-absolutely-does-not-exist-xyz987"));
    invalidSlugDuration.add(res.timings.duration);

    const is404 = res.status === 404;
    notFoundRate.add(is404);

    check(res, {
      "status 404":                   (_) => is404,
      "body is not 200 post schema":  (r) => { try { return JSON.parse(r.body).title === undefined || r.status !== 200; } catch { return true; } },
    });
  });

  sleep(0.5);

  // ── 3. Slug with special characters ──────────────────────────────────────
  group("3. Special characters in slug", () => {
    const cases = [
      { label: "exclamation mark",    slug: "hello!world"          },
      { label: "hash symbol",         slug: "post#section"         },
      { label: "ampersand",           slug: "a&b"                  },
      { label: "spaces (encoded)",    slug: "my%20post%20title"    },
      { label: "double slash",        slug: "cat//subcat"          },
      { label: "angle brackets",      slug: "<script>alert(1)</script>" },
      { label: "single quote",        slug: "it's-a-post"          },
    ];

    for (const c of cases) {
      const res = http.get(url(c.slug));
      invalidSlugDuration.add(res.timings.duration);

      check(res, {
        [`[${c.label}] no 500 error`]: (r) => r.status !== 500,
        [`[${c.label}] 400 or 404`]:   (r) => [400, 404].includes(r.status),
      });
    }
  });

  sleep(0.5);

  // ── 4. SQL injection-like slugs ───────────────────────────────────────────
  group("4. SQL injection patterns", () => {
    const cases = [
      "1'OR'1'='1",
      "1; DROP TABLE posts; --",
      "' OR 1=1 --",
      "admin'--",
    ];

    for (const slug of cases) {
      const res = http.get(url(encodeURIComponent(slug)));
      invalidSlugDuration.add(res.timings.duration);

      check(res, {
        [`[sqli: ${slug.slice(0, 20)}] no 500`]: (r) => r.status !== 500,
      });
    }
  });

  sleep(0.5);

  // ── 5. Path traversal attempts ────────────────────────────────────────────
  group("5. Path traversal attempts", () => {
    const cases = [
      "../secret",
      "../../etc/passwd",
      "%2e%2e%2fsecret",
    ];

    for (const slug of cases) {
      const res = http.get(url(slug));
      invalidSlugDuration.add(res.timings.duration);

      check(res, {
        [`[traversal: ${slug}] no 500`]:     (r) => r.status !== 500,
        [`[traversal: ${slug}] not 200`]:    (r) => r.status !== 200,
      });
    }
  });

  sleep(0.5);

  // ── 6. Unicode slug ───────────────────────────────────────────────────────
  group("6. Unicode slug", () => {
    const slugs = [
      "məqalə-başlığı",
      "日本語スラッグ",
      "пост-на-русском",
      "مقاله-فارسی",
    ];

    for (const slug of slugs) {
      const encoded = encodeURIComponent(slug);
      const res     = http.get(url(encoded));
      invalidSlugDuration.add(res.timings.duration);

      check(res, {
        [`[unicode: ${slug.slice(0, 10)}] no 500`]: (r) => r.status !== 500,
        [`[unicode: ${slug.slice(0, 10)}] 400 or 404`]: (r) => [400, 404].includes(r.status),
      });
    }
  });

  sleep(0.5);

  // ── 7. Very long slug ─────────────────────────────────────────────────────
  group("7. Extremely long slug (500 chars)", () => {
    const longSlug = "a".repeat(500);
    const res = http.get(url(longSlug));
    invalidSlugDuration.add(res.timings.duration);

    check(res, {
      "no 500 on long slug":         (r) => r.status !== 500,
      "400 or 404 on long slug":     (r) => [400, 404].includes(r.status),
    });
  });

  sleep(0.5);

  // ── 8. Numeric-only slug ──────────────────────────────────────────────────
  group("8. Numeric-only slug", () => {
    const res = http.get(url("12345"));

    check(res, {
      "no 500 on numeric slug":  (r) => r.status !== 500,
      "200 or 404":              (r) => [200, 404].includes(r.status),
    });
  });

  sleep(0.5);

  // ── 9. Case sensitivity ───────────────────────────────────────────────────
  group("9. Case sensitivity", () => {
    const upper  = http.get(url(knownSlug.toUpperCase()));
    const lower  = http.get(url(knownSlug.toLowerCase()));
    const mixed  = http.get(url(
      knownSlug.split("").map((c, i) => i % 2 === 0 ? c.toUpperCase() : c).join("")
    ));

    check(upper, { "UPPER slug → not 500": (r) => r.status !== 500 });
    check(lower, { "lower slug → 200":     (r) => r.status === 200 });
    check(mixed, { "MiXeD slug → not 500": (r) => r.status !== 500 });

    // Expectation: lowercase original should work, others likely 404
    const upperStatus = upper.status;
    const mixedStatus = mixed.status;
    check({ upperStatus, mixedStatus }, {
      "case-insensitive OR strict (no 500 either way)":
        () => upperStatus !== 500 && mixedStatus !== 500,
    });
  });

  sleep(0.5);

  // ── 10. Response headers ──────────────────────────────────────────────────
  group("10. Response headers on valid slug", () => {
    const res = http.get(url(knownSlug));

    check(res, {
      "Content-Type present":   (r) => !!r.headers["Content-Type"],
      "no server info leaked":  (r) => !r.headers["Server"]?.toLowerCase().includes("internal"),
      "body non-empty":         (r) => (r.body || "").length > 0,
    });
  });
}

// ═══════════════════════════════════════════════════════════════════════════
// SCENARIO B — Performance under load (happy path only)
// ═══════════════════════════════════════════════════════════════════════════

export function performanceScenario(data) {
  group("Perf — GET /api/posts/:slug", () => {
    const res = http.get(url(data.slug));
    validSlugDuration.add(res.timings.duration);

    check(res, {
      "status 200":       (r) => r.status === 200,
      "responded < 500ms":(r) => r.timings.duration < 500,
    });
  });

  sleep(1);
}

// ─── Teardown ──────────────────────────────────────────────────────────────

export function teardown(data) {
  if (data.createdBySetup && data.slug) {
    const res = http.del(`${BASE_URL}/api/posts/${data.slug}`);
    console.log(`Teardown: DELETE ${data.slug} → ${res.status}`);
  }
}