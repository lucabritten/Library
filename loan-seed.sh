#!/usr/bin/env bash
#
# Erzeugt Testdaten fuer die Ausleih-Metriken und zeigt danach die
# library_loans_*-Zeilen aus /actuator/prometheus.
#
# Ablauf:
#   1. Admin einloggen, einen Autor, ein Buch und mehrere Exemplare anlegen
#   2. Zwei Mitglieder (alice, bob) registrieren und einloggen
#   3. Ausleihen mit allen Ausgaengen: Erfolg, Limit erreicht, Exemplar vergeben
#   4. Eine Rueckgabe
#   5. Metriken ausgeben
#
# Voraussetzungen: curl, jq, laufende App, Admin-Konto in der Datenbank
#   (z. B. aus dem Dev-Seed im Profil "local").
#
# Aufruf:
#   ADMIN_PASSWORD='…' ./scripts/seed-loans.sh
#
# Optionale Variablen (mit Standardwert):
#   BASE_URL=http://localhost:8080     API
#   MGMT_URL=http://localhost:8081     Management-Port (nur lokal erreichbar,
#                                      in Docker nicht veroeffentlicht)
#   ADMIN_EMAIL=admin@admin.com
#   MAX_LOANS=5                        muss zu library.loans.max-active-loans passen

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
MGMT_URL="${MGMT_URL:-http://localhost:8081}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@admin.com}"
MAX_LOANS="${MAX_LOANS:-5}"
API="$BASE_URL/api/v1"
PASSWORD="correct-horse-battery"     # Testpasswort fuer die neuen Mitglieder
RUN_ID="$(date +%s)"                 # macht E-Mails eindeutig, Skript ist wiederholbar

for tool in curl jq; do
  command -v "$tool" >/dev/null || { echo "Fehlt: $tool" >&2; exit 1; }
done

if [[ -z "${ADMIN_PASSWORD:-}" ]]; then
  read -r -s -p "Passwort fuer $ADMIN_EMAIL: " ADMIN_PASSWORD
  echo
fi

# ---------------------------------------------------------------------------
# Hilfsfunktionen
# ---------------------------------------------------------------------------

# request METHOD PATH [TOKEN] [JSON]
# Setzt die globalen Variablen STATUS und BODY.
request() {
  local method="$1" path="$2" token="${3:-}" json="${4:-}"
  local args=(-s -w $'\n%{http_code}' -X "$method" "$API$path")
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$json"  ]] && args+=(-H "Content-Type: application/json" -d "$json")

  local out
  out="$(curl "${args[@]}")"
  STATUS="${out##*$'\n'}"
  BODY="${out%$'\n'*}"
}

# expect STATUS_ERWARTET BESCHREIBUNG
# Bricht ab, wenn der letzte Aufruf einen anderen Status geliefert hat.
expect() {
  if [[ "$STATUS" != "$1" ]]; then
    echo "FEHLER bei '$2': erwartet $1, bekommen $STATUS" >&2
    echo "$BODY" | jq . 2>/dev/null || echo "$BODY" >&2
    exit 1
  fi
  # nach stderr, damit "$(login …)" nur das Token einfaengt
  printf '  %-45s %s\n' "$2" "$STATUS" >&2
}

login() {
  request POST /auth/token "" "$(jq -n --arg e "$1" --arg p "$2" '{email:$e, password:$p}')"
  expect 200 "Login $1"
  jq -r .jwtToken <<<"$BODY"
}

register() {
  request POST /members "" "$(jq -n --arg n "$1" --arg e "$2" --arg p "$PASSWORD" '{name:$n, email:$e, password:$p}')"
  expect 201 "Registrierung $2"
}

lend() {
  request POST /loans "$1" "$(jq -n --arg i "$2" '{inventoryNumber:$i}')"
}

# Gueltige ISBN-13 mit Pruefziffer, eindeutig pro Lauf
unique_isbn() {
  local base sum=0 i digit
  base="978$(printf '%09d' $(( RUN_ID % 1000000000 )))"
  for (( i = 0; i < 12; i++ )); do
    digit="${base:i:1}"
    (( sum += (i % 2 == 0) ? digit : digit * 3 ))
  done
  echo "$base$(( (10 - sum % 10) % 10 ))"
}

# ---------------------------------------------------------------------------
# 1. Katalog als Admin anlegen
# ---------------------------------------------------------------------------
echo "Katalog"
ADMIN_TOKEN="$(login "$ADMIN_EMAIL" "$ADMIN_PASSWORD")"

request POST /authors "$ADMIN_TOKEN" '{"firstName":"Michael","lastName":"Ende"}'
expect 201 "Autor angelegt"
AUTHOR_ID="$(jq -r .id <<<"$BODY")"

request POST /books "$ADMIN_TOKEN" \
  "$(jq -n --arg isbn "$(unique_isbn)" --argjson a "$AUTHOR_ID" '{title:"Momo", isbn:$isbn, authorId:$a}')"
expect 201 "Buch angelegt"
BOOK_ID="$(jq -r .id <<<"$BODY")"

COPIES=()
for (( i = 1; i <= MAX_LOANS + 2; i++ )); do
  request POST /copies "$ADMIN_TOKEN" "$(jq -n --argjson b "$BOOK_ID" '{bookId:$b}')"
  expect 201 "Exemplar $i angelegt"
  COPIES+=("$(jq -r .inventoryNumber <<<"$BODY")")
done

# ---------------------------------------------------------------------------
# 2. Mitglieder
# ---------------------------------------------------------------------------
echo "Mitglieder"
ALICE="alice-$RUN_ID@example.com"
BOB="bob-$RUN_ID@example.com"
register "Alice" "$ALICE"
register "Bob" "$BOB"
ALICE_TOKEN="$(login "$ALICE" "$PASSWORD")"
BOB_TOKEN="$(login "$BOB" "$PASSWORD")"

# ---------------------------------------------------------------------------
# 3. Ausleihen mit allen Ausgaengen
# ---------------------------------------------------------------------------
echo "Ausleihen"
ALICE_LOANS=()
for (( i = 0; i < MAX_LOANS; i++ )); do
  lend "$ALICE_TOKEN" "${COPIES[$i]}"
  expect 201 "Alice leiht ${COPIES[$i]} (succeeded)"
  ALICE_LOANS+=("$(jq -r .id <<<"$BODY")")
done

lend "$ALICE_TOKEN" "${COPIES[$MAX_LOANS]}"
expect 409 "Alice ueber dem Limit (loan-limit-exceeded)"

lend "$BOB_TOKEN" "${COPIES[0]}"
expect 409 "Bob, Exemplar vergeben (copy-not-available)"

lend "$BOB_TOKEN" "${COPIES[$MAX_LOANS]}"
expect 201 "Bob leiht ${COPIES[$MAX_LOANS]} (succeeded)"

# ---------------------------------------------------------------------------
# 4. Eine Rueckgabe
# ---------------------------------------------------------------------------
echo "Rueckgabe"
request POST "/loans/${ALICE_LOANS[0]}/return" "$ALICE_TOKEN"
expect 200 "Alice gibt Ausleihe ${ALICE_LOANS[0]} zurueck"

# ---------------------------------------------------------------------------
# 5. Metriken
# ---------------------------------------------------------------------------
echo
echo "Metriken von $MGMT_URL/actuator/prometheus"
if metrics="$(curl -sf "$MGMT_URL/actuator/prometheus")"; then
  grep '^library_loans' <<<"$metrics" || echo "  Keine library_loans-Zeilen gefunden."
else
  echo "  Management-Port nicht erreichbar. Laeuft die App in Docker, so pruefen:"
  echo "  docker compose exec app curl -s localhost:8081/actuator/prometheus | grep '^library_loans'"
fi