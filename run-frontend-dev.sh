#!/usr/bin/env bash
set -euo pipefail
cd /srv/projects/order/vue-admin-new-code
export NODE_OPTIONS=--max-old-space-size=1536
printf '%s\n' 'Development URL through SSH tunnel: http://localhost:5171'
exec npm run dev -- --host 127.0.0.1 --port 5171 --strictPort "$@"
