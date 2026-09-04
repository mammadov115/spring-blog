/**
 * k6 test — PUT /api/posts/{slug}
 *
 * Run:
 *   k6 run put-post-by-slug.k6.js
 *   k6 run -e BASE_URL=http://your-host:8080 put-post-by-slug.k6.js
 *   k6 run -e LOG_PERF=true put-post-by-slug.k6.js
 */

import http from "k6/http";
import { check, group, sleep } from "k6";
import { Trend, Rate, Counter } from "k6/metrics";

// ─── Config ────────────────────────────────────────────────────────────────

const BASE_URL  = __ENV.BASE_URL  || "http://localhost:8080";
const LOG_PERF  = __ENV.LOG_PERF  === "true";

// ─── Custom metrics ────────────────────────────────────────────────────────

const updateDuration      = new Trend("duration_update_success", true);
const updateErrorDuration = new Trend("duration_update_error",   true);
const schemaErrors        = new Counter("schema_errors");
const updateSuccessRate   = new Rate("update_success_rate");

// ─── Logger ────────────────────────────────────────────────────────────────

function logHttp(label, method, url, reqBody, res) {
  let reqDisplay = "(no body)";
  if (reqBody) {
    try { reqDisplay = JSON.stringify(JSON.parse(reqBody), null, 2); }
    catch { reqDisplay = reqBody.length > 300 ? reqBody.slice(0, 300) + "…" : reqBody; }
  }

  let resDisplay = "(empty body)";
  if (res.body) {
    try { resDisplay = JSON.stringify(JSON.parse(res.body), null, 2); }
    catch { resDisplay = res.body.length > 500 ? res.body.slice(0, 500) + "…" : res.body; }
  }

  const emoji = res.status >= 500 ? "🔴" : res.status >= 400 ? "🟡" : "🟢";

  console.log(
    `\n${"─".repeat(60)}\n` +
    `📤 [${label}] ${method} ${url}\n` +
    `   REQ: ${reqDisplay}\n` +
    `📥 ${emoji} ${res.status} (${res.timings.duration.toFixed(1)}ms)\n` +
    `   RES: ${resDisplay}\n` +
    `${"─".repeat(60)}`
  );
}

// ─── Helpers ───────────────────────────────────────────────────────────────

const JSON_HEADERS = { "Content-Type": "application/json" };
const TEXT_HEADERS = { "Content-Type": "text/plain" };

function putUrl(slug) {
  return `${BASE_URL}/api/posts/${slug}`;
}

function validPayload(overrides = {}) {
  return JSON.stringify({
    title:  "Updated Title",
    slug:   "will-be-overridden",
    body:   "Updated body content.",
    status: "PUBLISHED",
    ...overrides,
  });
}

function validSchema(r) {
  try {
    const b = JSON.parse(r.body);
    return (
      typeof b.id     === "number" &&
      typeof b.title  === "string" &&
      typeof b.slug   === "string" &&
      typeof b.body   === "string" &&
      typeof b.status === "string" &&
      Array.isArray(b.tags)
    );
  } catch { return false; }
}

function validPublishDate(r) {
  try {
    const val = JSON.parse(r.body).publish;
    if (val === null || val === undefined) return true;
    return !isNaN(Date.parse(val));
  } catch { return false; }
}

function createPost(slug, status = "DRAFT") {
  return http.post(
    `${BASE_URL}/api/posts`,
    JSON.stringify({ title: `Setup post — ${slug}`, slug, body: "Original body content.", status }),
    { headers: JSON_HEADERS }
  );
}

function deletePost(slug) {
  return http.del(`${BASE_URL}/api/posts/${slug}`);
}

// ─── Setup ─────────────────────────────────────────────────────────────────

export function setup() {
  const slugs = {
    main:        `k6-put-main-${Date.now()}`,
    idempotent:  `k6-put-idem-${Date.now()}`,
    statusCycle: `k6-put-stat-${Date.now()}`,
    perf:        `k6-put-perf-${Date.now()}`,
  };

  for (const [key, slug] of Object.entries(slugs)) {
    const res = createPost(slug);
    if (res.status !== 200 && res.status !== 201) {
      console.warn(`Setup: failed to create "${key}" post (${res.status}): ${res.body}`);
    } else {
      console.log(`Setup: created "${key}" → slug="${slug}"`);
    }
  }

  return { slugs };
}

// ─── Options ───────────────────────────────────────────────────────────────

export const options = {
  scenarios: {
    functional: {
      executor:    "shared-iterations",
      vus:         1,
      iterations:  1,
      maxDuration: "60s",
      exec:        "functionalScenario",
    },
    performance: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "15s", target: 10 },
        { duration: "30s", target: 20 },
        { duration: "15s", target:  0 },
      ],
      startTime: "70s",
      exec:      "performanceScenario",
    },
  },

  thresholds: {
    "http_req_failed{expected_response:false}": ["rate<0.01"],
    duration_update_success: ["p(95)<800", "p(99)<1500"],
    duration_update_error:   ["p(95)<400"],
    update_success_rate:     ["rate>0.95"],
    schema_errors:           ["count==0"],
  },
};

// ═══════════════════════════════════════════════════════════════════════════
// SCENARIO A — Functional tests
// ═══════════════════════════════════════════════════════════════════════════

export function functionalScenario(data) {
  const { slugs } = data;

  // ── 1. Happy path ─────────────────────────────────────────────────────────
  group("1. Happy path — full valid update", () => {
    const body = validPayload({ title: "Fully Updated Title", body: "Fully updated body content.", status: "PUBLISHED" });
    const url  = putUrl(slugs.main);
    const res  = http.put(url, body, { headers: JSON_HEADERS });
    updateDuration.add(res.timings.duration);
    logHttp("1. Happy path", "PUT", url, body, res);

    const schemaOk = validSchema(res);
    if (!schemaOk) schemaErrors.add(1);

    const ok = check(res, {
      "status 200":             (r) => r.status === 200,
      "content-type is json":   (r) => (r.headers["Content-Type"] || "").includes("application/json"),
      "schema valid":           (_) => schemaOk,
      "title updated":          (r) => { try { return JSON.parse(r.body).title === "Fully Updated Title"; } catch { return false; } },
      "body updated":           (r) => { try { return JSON.parse(r.body).body  === "Fully updated body content."; } catch { return false; } },
      "status updated":         (r) => { try { return JSON.parse(r.body).status === "PUBLISHED"; } catch { return false; } },
      "publish is valid date":  (r) => validPublishDate(r),
      "tags is array":          (r) => { try { return Array.isArray(JSON.parse(r.body).tags); } catch { return false; } },
      "id is positive integer": (r) => { try { const b = JSON.parse(r.body); return Number.isInteger(b.id) && b.id > 0; } catch { return false; } },
    });
    updateSuccessRate.add(ok);
  });

  sleep(0.5);

  // ── 2. Partial update — only title ────────────────────────────────────────
  group("2. Partial update — only title changed", () => {
    const body = validPayload({ title: "Only Title Changed" });
    const url  = putUrl(slugs.main);
    const res  = http.put(url, body, { headers: JSON_HEADERS });
    updateDuration.add(res.timings.duration);
    logHttp("2. Partial update", "PUT", url, body, res);

    check(res, {
      "status 200":         (r) => r.status === 200,
      "title reflects new": (r) => { try { return JSON.parse(r.body).title === "Only Title Changed"; } catch { return false; } },
    });
  });

  sleep(0.5);

  // ── 3. Status cycle ───────────────────────────────────────────────────────
  group("3. Status cycle — DRAFT → PUBLISHED → ARCHIVED", () => {
    for (const status of ["DRAFT", "PUBLISHED", "ARCHIVED"]) {
      const body = validPayload({ status });
      const url  = putUrl(slugs.statusCycle);
      const res  = http.put(url, body, { headers: JSON_HEADERS });
      updateDuration.add(res.timings.duration);
      logHttp(`3. Status=${status}`, "PUT", url, body, res);

      check(res, {
        [`status → ${status} : 200`]:      (r) => r.status === 200,
        [`status → ${status} : reflects`]: (r) => { try { return JSON.parse(r.body).status === status; } catch { return false; } },
      });
      sleep(0.2);
    }
  });

  sleep(0.5);

  // ── 4. Body slug vs path slug conflict ────────────────────────────────────
  group("4. Slug in body differs from path slug", () => {
    const body = validPayload({ slug: "completely-different-slug" });
    const url  = putUrl(slugs.main);
    const res  = http.put(url, body, { headers: JSON_HEADERS });
    updateDuration.add(res.timings.duration);
    logHttp("4. Slug conflict", "PUT", url, body, res);

    check(res, {
      "no 500":                     (r) => r.status !== 500,
      "200 or 400":                 (r) => [200, 400].includes(r.status),
      "path slug preserved if 200": (r) => {
        if (r.status !== 200) return true;
        try { return JSON.parse(r.body).slug !== "completely-different-slug"; }
        catch { return false; }
      },
    });
  });

  sleep(0.5);

  // ── 5. Idempotency ────────────────────────────────────────────────────────
  group("5. Idempotency — same payload sent twice", () => {
    const body = validPayload({ title: "Idempotent Title", status: "PUBLISHED" });
    const url  = putUrl(slugs.idempotent);

    const res1 = http.put(url, body, { headers: JSON_HEADERS });
    updateDuration.add(res1.timings.duration);
    logHttp("5. Idempotency 1st", "PUT", url, body, res1);

    const res2 = http.put(url, body, { headers: JSON_HEADERS });
    updateDuration.add(res2.timings.duration);
    logHttp("5. Idempotency 2nd", "PUT", url, body, res2);

    check(res1, { "1st call → 200": (r) => r.status === 200 });
    check(res2, { "2nd call → 200": (r) => r.status === 200 });
    check({ r1: res1, r2: res2 }, {
      "both return same status code": ({ r1, r2 }) => r1.status === r2.status,
      "both return same title":       ({ r1, r2 }) => {
        try { return JSON.parse(r1.body).title === JSON.parse(r2.body).title; }
        catch { return false; }
      },
    });
  });

  sleep(0.5);

  // ── 6. Non-existent slug ──────────────────────────────────────────────────
  group("6. Non-existent slug → 404", () => {
    const body = validPayload();
    const url  = putUrl("slug-that-does-not-exist-xyz987");
    const res  = http.put(url, body, { headers: JSON_HEADERS });
    updateErrorDuration.add(res.timings.duration);
    logHttp("6. Non-existent slug", "PUT", url, body, res);

    check(res, {
      "status 404": (r) => r.status === 404,
      "no 500":     (r) => r.status !== 500,
    });
  });

  sleep(0.5);

  // ── 7. Missing required fields ────────────────────────────────────────────
  group("7. Validation — missing required fields", () => {
    const cases = [
      { label: "missing title",  payload: { body: "some body", status: "DRAFT" } },
      { label: "missing body",   payload: { title: "some title", status: "DRAFT" } },
      { label: "missing status", payload: { title: "some title", body: "some body" } },
      { label: "empty object",   payload: {} },
    ];

    for (const c of cases) {
      const body = JSON.stringify(c.payload);
      const url  = putUrl(slugs.main);
      const res  = http.put(url, body, { headers: JSON_HEADERS });
      updateErrorDuration.add(res.timings.duration);
      logHttp(`7. ${c.label}`, "PUT", url, body, res);

      check(res, {
        [`[${c.label}] no 500`]:     (r) => r.status !== 500,
        [`[${c.label}] 400 or 422`]: (r) => [400, 422].includes(r.status),
      });
    }
  });

  sleep(0.5);

  // ── 8. Empty string values ────────────────────────────────────────────────
  group("8. Validation — empty string values", () => {
    const cases = [
      { label: "empty title", payload: { title: "",  body: "valid body",   status: "DRAFT" } },
      { label: "empty body",  payload: { title: "valid title", body: "",   status: "DRAFT" } },
    ];

    for (const c of cases) {
      const body = JSON.stringify(c.payload);
      const url  = putUrl(slugs.main);
      const res  = http.put(url, body, { headers: JSON_HEADERS });
      updateErrorDuration.add(res.timings.duration);
      logHttp(`8. ${c.label}`, "PUT", url, body, res);

      check(res, {
        [`[${c.label}] no 500`]:     (r) => r.status !== 500,
        [`[${c.label}] 400 or 422`]: (r) => [400, 422].includes(r.status),
      });
    }
  });

  sleep(0.5);

  // ── 9. Invalid status values ──────────────────────────────────────────────
  group("9. Validation — invalid status values", () => {
    const cases = [
      { label: "lowercase published", value: "published" },
      { label: "random string",       value: "INVALID"   },
      { label: "number as status",    value: "1"         },
      { label: "empty status",        value: ""          },
      { label: "null status",         value: null        },
    ];

    for (const c of cases) {
      const body = JSON.stringify({ title: "title", body: "body", status: c.value });
      const url  = putUrl(slugs.main);
      const res  = http.put(url, body, { headers: JSON_HEADERS });
      updateErrorDuration.add(res.timings.duration);
      logHttp(`9. status="${c.label}"`, "PUT", url, body, res);

      check(res, {
        [`[status: ${c.label}] no 500`]:     (r) => r.status !== 500,
        [`[status: ${c.label}] 400 or 422`]: (r) => [400, 422].includes(r.status),
      });
    }
  });

  sleep(0.5);

  // ── 10. Wrong Content-Type ────────────────────────────────────────────────
  group("10. Wrong / missing Content-Type", () => {
    const raw = `{"title":"test","body":"body","status":"DRAFT"}`;
    const url = putUrl(slugs.main);

    const noCT = http.put(url, raw, { headers: {} });
    logHttp("10. No Content-Type", "PUT", url, raw, noCT);
    check(noCT, {
      "no Content-Type → no 500":     (r) => r.status !== 500,
      "no Content-Type → 400 or 415": (r) => [400, 415].includes(r.status),
    });

    const txtPlain = http.put(url, raw, { headers: TEXT_HEADERS });
    logHttp("10. text/plain", "PUT", url, raw, txtPlain);
    check(txtPlain, {
      "text/plain → no 500":     (r) => r.status !== 500,
      "text/plain → 400 or 415": (r) => [400, 415].includes(r.status),
    });
  });

  sleep(0.5);

  // ── 11. Malformed JSON ────────────────────────────────────────────────────
  group("11. Malformed JSON body", () => {
    const cases = [
      { label: "truncated json",    body: `{"title":"test"` },
      { label: "plain text",        body: "this is not json" },
      { label: "array instead obj", body: `["title","body"]` },
    ];

    for (const c of cases) {
      const url = putUrl(slugs.main);
      const res = http.put(url, c.body, { headers: JSON_HEADERS });
      updateErrorDuration.add(res.timings.duration);
      logHttp(`11. ${c.label}`, "PUT", url, c.body, res);

      check(res, {
        [`[${c.label}] no 500`]: (r) => r.status !== 500,
        [`[${c.label}] 400`]:    (r) => r.status === 400,
      });
    }
  });

  sleep(0.5);

  // ── 12. Very long field values ────────────────────────────────────────────
  group("12. Boundary — very long field values", () => {
    const url = putUrl(slugs.main);

    const longTitleBody = JSON.stringify({ title: "T".repeat(5000), body: "normal body", status: "DRAFT" });
    const resLT = http.put(url, longTitleBody, { headers: JSON_HEADERS });
    logHttp("12. Long title (5000)", "PUT", url, `{"title":"TTT…[x5000]","body":"normal body","status":"DRAFT"}`, resLT);
    check(resLT, {
      "long title → no 500":     (r) => r.status !== 500,
      "long title → 200 or 400": (r) => [200, 400].includes(r.status),
    });

    const longBodyPayload = JSON.stringify({ title: "normal title", body: "B".repeat(50000), status: "DRAFT" });
    const resLB = http.put(url, longBodyPayload, { headers: JSON_HEADERS });
    logHttp("12. Long body (50000)", "PUT", url, `{"title":"normal title","body":"BBB…[x50000]","status":"DRAFT"}`, resLB);
    check(resLB, {
      "long body → no 500":     (r) => r.status !== 500,
      "long body → 200 or 400": (r) => [200, 400].includes(r.status),
    });
  });

  sleep(0.5);

  // ── 13. Special chars / injection ────────────────────────────────────────
  group("13. Special chars / injection in title & body", () => {
    const cases = [
      { label: "html in title",   title: "<h1>Injected</h1>",    body: "normal" },
      { label: "sql in title",    title: "'; DROP TABLE posts;", body: "normal" },
      { label: "emoji in title",  title: "Post 🚀🔥💥",          body: "normal" },
      { label: "newline in body", title: "normal",               body: "line1\nline2\nline3" },
      { label: "null byte",       title: "title\x00hidden",      body: "body" },
    ];

    for (const c of cases) {
      const body = JSON.stringify({ title: c.title, body: c.body, status: "DRAFT" });
      const url  = putUrl(slugs.main);
      const res  = http.put(url, body, { headers: JSON_HEADERS });
      updateDuration.add(res.timings.duration);
      logHttp(`13. ${c.label}`, "PUT", url, body, res);

      check(res, {
        [`[${c.label}] no 500`]:     (r) => r.status !== 500,
        [`[${c.label}] 200 or 400`]: (r) => [200, 400].includes(r.status),
      });
    }
  });
}

// ═══════════════════════════════════════════════════════════════════════════
// SCENARIO B — Performance + race condition
// ═══════════════════════════════════════════════════════════════════════════

export function performanceScenario(data) {
  const { slugs } = data;

  group("Perf — concurrent PUT /api/posts/:slug", () => {
    const body = validPayload({ title: "Concurrent update by VU", status: "PUBLISHED" });
    const url  = putUrl(slugs.perf);
    const res  = http.put(url, body, { headers: JSON_HEADERS });
    updateDuration.add(res.timings.duration);

    if (LOG_PERF) logHttp("Perf", "PUT", url, body, res);

    const ok = check(res, {
      "status 200":       (r) => r.status === 200,
      "no 500":           (r) => r.status !== 500,
      "schema valid":     (r) => validSchema(r),
      "response < 800ms": (r) => r.timings.duration < 800,
    });

    updateSuccessRate.add(ok);
  });

  sleep(1);
}

// ─── Teardown ──────────────────────────────────────────────────────────────

export function teardown(data) {
  for (const [key, slug] of Object.entries(data.slugs)) {
    const res = deletePost(slug);
    console.log(`Teardown: DELETE "${key}" (${slug}) → ${res.status}`);
  }
}