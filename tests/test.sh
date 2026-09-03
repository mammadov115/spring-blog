#!/bin/bash

BASE_URL="http://localhost:8080"
SLUG="left-gun-who-adf6d875"
POST_ID="14730"

run_test() {
    local name=$1
    local url=$2
    
    result=$(curl -s -o /dev/null -w "%{time_total} %{http_code}" "$url")
    time=$(echo $result | awk '{print $1}')
    status=$(echo $result | awk '{print $2}')
    
    printf "%-45s | ⏱ %-8s | %s\n" "$name" "${time}s" "HTTP $status"
}

echo "=================================================="
echo "🚀 Blog API Test"
echo "=================================================="
echo ""

run_test "GET /api/posts (pagination)"        "$BASE_URL/api/posts?page=0&size=20"
run_test "GET /api/posts (page 100)"          "$BASE_URL/api/posts?page=100&size=20"
run_test "GET /api/posts (page 5000)"         "$BASE_URL/api/posts?page=5000&size=20"
run_test "GET /api/posts/{slug}"              "$BASE_URL/api/posts/$SLUG"
run_test "GET /api/posts/keyset"              "$BASE_URL/api/posts/keyset?size=10"
run_test "GET /api/posts/keyset (cursor)"     "$BASE_URL/api/posts/keyset?cursor=400000&size=10"
run_test "GET /api/posts/{slug}/tags"         "$BASE_URL/api/posts/$SLUG/tags"
run_test "GET /api/posts/{slug}/similar"      "$BASE_URL/api/posts/$SLUG/similar"
run_test "GET /api/posts/{id}/comments"       "$BASE_URL/api/posts/$POST_ID/comments"
run_test "GET /api/tags/{tag}/posts"          "$BASE_URL/api/tags/high/posts"
run_test "GET /sitemap.xml"                   "$BASE_URL/sitemap.xml"
run_test "GET /feed/rss"                      "$BASE_URL/feed/rss"

echo ""
echo "=================================================="