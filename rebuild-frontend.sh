#!/usr/bin/env bash
set -euo pipefail
cd /srv/projects/order/vue-admin-new-code
export NODE_OPTIONS=--max-old-space-size=1536
npm run build:prod
mkdir -p dist/config
printf '%s\n' 'window.APP_CONFIG = { baseApiUrl: "/prod-api" };' > dist/config/config.js
