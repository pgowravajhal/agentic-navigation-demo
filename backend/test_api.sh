#!/bin/bash
# Comprehensive API test script
# Usage: bash test_api.sh [base_url]

BASE_URL="${1:-http://localhost:8000}"
PASS=0
FAIL=0

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║         Agentic Navigator — Backend API Tests               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo "Backend: $BASE_URL"
echo ""

# --- Health ---
echo "━━━ Health ━━━"
echo -n "  1. GET /health returns 200 ... "
HEALTH=$(curl -s "$BASE_URL/health")
STATUS=$(echo "$HEALTH" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)
if [ "$STATUS" = "healthy" ]; then echo "PASS ✓"; ((PASS++)); else echo "FAIL ✗ (status=$STATUS)"; ((FAIL++)); fi

echo -n "  2. Health reports mode ... "
MODE=$(echo "$HEALTH" | python3 -c "import sys,json; print(json.load(sys.stdin)['mode'])" 2>/dev/null)
if [ -n "$MODE" ]; then echo "PASS ✓ (mode=$MODE)"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

echo -n "  3. Health lists services ... "
SVCS=$(echo "$HEALTH" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('services',{})))" 2>/dev/null)
if [ "$SVCS" -ge 4 ] 2>/dev/null; then echo "PASS ✓ ($SVCS services)"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

# --- Recommend Route ---
echo ""
echo "━━━ POST /recommend-route ━━━"
RESPONSE=$(curl -s -X POST "$BASE_URL/recommend-route" \
    -H "Content-Type: application/json" \
    -d '{"origin":{"latitude":52.52,"longitude":13.405,"label":"Berlin"},"destination":{"latitude":48.8566,"longitude":2.3522,"label":"Paris"},"preferences":{"transport_mode":"car","prioritize":"fastest"}}')

echo -n "  4. Returns request_id ... "
REQ_ID=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['request_id'])" 2>/dev/null)
if [ -n "$REQ_ID" ]; then echo "PASS ✓ ($REQ_ID)"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

echo -n "  5. Has recommended_route ... "
REC=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['recommended_route']['label'])" 2>/dev/null)
if [ -n "$REC" ]; then echo "PASS ✓"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

echo -n "  6. Has alternatives ... "
ALTS=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['alternatives']))" 2>/dev/null)
if [ "$ALTS" -ge 1 ] 2>/dev/null; then echo "PASS ✓ ($ALTS alternatives)"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

echo -n "  7. Has explanation with factors ... "
FACTORS=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['explanation']['factors']))" 2>/dev/null)
if [ "$FACTORS" -ge 2 ] 2>/dev/null; then echo "PASS ✓ ($FACTORS factors)"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

echo -n "  8. Has confidence score ... "
CONF=$(echo "$RESPONSE" | python3 -c "import sys,json; c=json.load(sys.stdin)['explanation']['confidence']; assert 0<c<=1; print(f'{c:.2f}')" 2>/dev/null)
if [ -n "$CONF" ]; then echo "PASS ✓ ($CONF)"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

echo -n "  9. Has traffic_summary ... "
TRAFFIC=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['traffic_summary']['level'])" 2>/dev/null)
if [ -n "$TRAFFIC" ]; then echo "PASS ✓ ($TRAFFIC)"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

echo -n " 10. Has weather_summary ... "
WEATHER=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['weather_summary']['conditions'][:20])" 2>/dev/null)
if [ -n "$WEATHER" ]; then echo "PASS ✓"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

echo -n " 11. Has POIs ... "
POIS=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['pois']))" 2>/dev/null)
if [ "$POIS" -ge 1 ] 2>/dev/null; then echo "PASS ✓ ($POIS POIs)"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

echo -n " 12. Has ETA in route summary ... "
ETA=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['recommended_route']['summary']['eta'])" 2>/dev/null)
if [ -n "$ETA" ] && [ "$ETA" != "null" ]; then echo "PASS ✓ (ETA=$ETA)"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

echo -n " 13. Has 5 agents in trace ... "
AGENTS=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin)['agent_trace']['agents']))" 2>/dev/null)
if [ "$AGENTS" -eq 5 ] 2>/dev/null; then echo "PASS ✓"; ((PASS++)); else echo "FAIL ✗ (got $AGENTS)"; ((FAIL++)); fi

echo -n " 14. Has poi scores ... "
POI_SCORE=$(echo "$RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['recommended_route']['scores']['poi'])" 2>/dev/null)
if [ -n "$POI_SCORE" ]; then echo "PASS ✓ ($POI_SCORE)"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

# --- Agent Trace ---
echo ""
echo "━━━ GET /agent-trace ━━━"
echo -n " 15. Trace exists for request ... "
TRACE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/agent-trace/$REQ_ID")
if [ "$TRACE_STATUS" = "200" ]; then echo "PASS ✓"; ((PASS++)); else echo "FAIL ✗ ($TRACE_STATUS)"; ((FAIL++)); fi

echo -n " 16. Trace 404 for unknown id ... "
TRACE_404=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/agent-trace/nonexistent")
if [ "$TRACE_404" = "404" ]; then echo "PASS ✓"; ((PASS++)); else echo "FAIL ✗ ($TRACE_404)"; ((FAIL++)); fi

# --- Demo Reset ---
echo ""
echo "━━━ POST /demo/reset ━━━"
echo -n " 17. Reset with valid scenario ... "
RESET=$(curl -s -X POST "$BASE_URL/demo/reset" -H "Content-Type: application/json" -d '{"mode":"mock","scenario":"default"}')
RESET_STATUS=$(echo "$RESET" | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])" 2>/dev/null)
if [ "$RESET_STATUS" = "reset_complete" ]; then echo "PASS ✓"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

echo -n " 18. Reset rejects invalid scenario ... "
INVALID=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/demo/reset" -H "Content-Type: application/json" -d '{"scenario":"bogus"}')
if [ "$INVALID" = "400" ]; then echo "PASS ✓"; ((PASS++)); else echo "FAIL ✗ ($INVALID)"; ((FAIL++)); fi

echo -n " 19. Reset clears traces ... "
# Make a request, then reset, then check old trace is gone
curl -s -X POST "$BASE_URL/recommend-route" -H "Content-Type: application/json" \
    -d '{"origin":{"latitude":52.52,"longitude":13.405},"destination":{"latitude":48.85,"longitude":2.35}}' > /dev/null
curl -s -X POST "$BASE_URL/demo/reset" -H "Content-Type: application/json" -d '{}' > /dev/null
OLD_TRACE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/agent-trace/$REQ_ID")
if [ "$OLD_TRACE" = "404" ]; then echo "PASS ✓ (traces cleared)"; ((PASS++)); else echo "FAIL ✗ (trace still exists)"; ((FAIL++)); fi

# --- OpenAPI ---
echo ""
echo "━━━ OpenAPI / Swagger ━━━"
echo -n " 20. Swagger UI accessible ... "
DOCS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/docs")
if [ "$DOCS" = "200" ]; then echo "PASS ✓"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

echo -n " 21. OpenAPI JSON accessible ... "
OPENAPI=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/openapi.json")
if [ "$OPENAPI" = "200" ]; then echo "PASS ✓"; ((PASS++)); else echo "FAIL ✗"; ((FAIL++)); fi

# --- Validation ---
echo ""
echo "━━━ Input Validation ━━━"
echo -n " 22. Rejects invalid latitude ... "
INVALID_LAT=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/recommend-route" \
    -H "Content-Type: application/json" \
    -d '{"origin":{"latitude":999,"longitude":13.405},"destination":{"latitude":48.85,"longitude":2.35}}')
if [ "$INVALID_LAT" = "422" ]; then echo "PASS ✓"; ((PASS++)); else echo "FAIL ✗ ($INVALID_LAT)"; ((FAIL++)); fi

echo -n " 23. Rejects missing destination ... "
MISSING=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/recommend-route" \
    -H "Content-Type: application/json" \
    -d '{"origin":{"latitude":52.52,"longitude":13.405}}')
if [ "$MISSING" = "422" ]; then echo "PASS ✓"; ((PASS++)); else echo "FAIL ✗ ($MISSING)"; ((FAIL++)); fi

# --- Summary ---
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
printf "║  Results: %d passed, %d failed                              ║\n" $PASS $FAIL
echo "╚══════════════════════════════════════════════════════════════╝"

if [ $FAIL -gt 0 ]; then exit 1; fi
