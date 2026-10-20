#!/usr/bin/env bash
set -euo pipefail

SUITES="Infrastructure Sep10 Sep12 Sep38 Sep6 Sep24 Sep31 Sep45 Platform"
RPC="Platform Sep6 Sep24 Sep31 Sep38"

RULES=(
  "^(docs|helm-charts|extended-tests)/|\.md$|/src/test/kotlin/|^(LICENSE|icon\.png)$ =>"
  "^essential-tests/.*/(ServerHealth|Kafka|LedgerClient)Tests\.kt$ => Infrastructure"
  "^essential-tests/.*/(Callback[A-Za-z]*|ClientConfigApiTests|EventProcessingTests|MultiClientCallbackTests|PaymentObserverTests|StellarObserverTests|WalletCallbackRecordTests|PlatformApiTests)\.kt$ => Platform"
  "[Ss]ep6[/A-Z] => Sep6 Sep38 Platform"
  "[Ss]ep(12|9)[/A-Z]|callbacks/customer/|RestCustomerIntegration|NotifyCustomerInfoUpdated => Sep12 Sep6 Sep24 Sep31 Sep38 Platform"
  "[Ss]ep24[/A-Z]|callbacks/interactive/|NotifyInteractiveFlowCompleted => Sep24 Sep38 Platform"
  "[Ss]ep31[/A-Z] => Sep31 Sep38 Platform"
  "[Ss]ep38[/A-Z]|callbacks/rate/|RestRateIntegration|GetQuoteHandler => Sep38 Sep6 Sep24 Sep31 Platform"
  "[Ss]ep45[/A-Z] => Sep45 Sep6 Sep24"
  "^platform/.*/platform/(rpc|observer|callback|controller/platform)/|^api-schema/.*/api/(rpc|platform|callback)/ => $RPC"
)

suites_for() {
  local rule

  for rule in "${RULES[@]}"; do
    if [[ $1 =~ ${rule%% =>*} ]]; then
      hit=${rule#*=>}

      return
    fi
  done

  hit=$SUITES
}

with_validation() {
  if [[ " $1 " =~ \ Sep(6|10|12|24|31|38)\  ]]; then echo "$1 validation"; else echo "$1"; fi
}

select_suites() {
  local picked="" out="" f s

  while read -r f; do
    suites_for "$f"
    picked="$picked $hit"

  done < <(git diff --no-renames --name-only "$(git merge-base "$1" HEAD)" HEAD)

  for s in $SUITES; do
    if [[ " $picked " == *" $s "* ]]; then out="$out $s"; fi
  done

  with_validation "${out# }"
}

fingerprints() {
  local tmp meta path s

  tmp=$(mktemp -d)

  for s in $SUITES; do : > "$tmp/$s"; done

  while IFS=$'\t' read -r meta path; do
    suites_for "$path"

    for s in $hit; do echo "$meta $path" >> "$tmp/$s"; done
  done < <(git ls-files -s)

  for s in $SUITES; do echo "$s $(sha256sum < "$tmp/$s" | cut -c1-16)"; done > "$tmp/all"

  cat "$tmp/all"
  echo "validation $(grep -E '^Sep(6|10|12|24|31|38) ' "$tmp/all" | sha256sum | cut -c1-16)"

  rm -rf "$tmp"
}

case "${1:-}" in
  select) select_suites "$2" ;;

  fingerprints) fingerprints ;;
  
  *) echo "usage: essential-suites.sh select <base> | fingerprints" >&2; exit 1 ;;
esac
