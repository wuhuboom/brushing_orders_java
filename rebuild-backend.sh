#!/usr/bin/env bash
set -euo pipefail
cd /srv/projects/order/java-admin-new-code
export MAVEN_OPTS=-Xmx768m
mvn -B -Dmaven.test.skip=true package
sudo /usr/bin/systemctl restart order.service
sudo /usr/bin/systemctl status order.service --no-pager
