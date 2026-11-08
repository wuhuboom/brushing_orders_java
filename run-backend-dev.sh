#!/usr/bin/env bash
set -euo pipefail
if systemctl is-active --quiet order.service; then
  printf '%s\n' 'First stop the background service: sudo systemctl stop order.service' >&2
  exit 1
fi
cd /srv/projects/order/java-admin-new-code
export MAVEN_OPTS=-Xmx768m
export SPRING_CONFIG_ADDITIONAL_LOCATION=file:/etc/project-env/order.yml
mvn -B -Dmaven.test.skip=true install
cd order-admin
exec mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Duser.timezone=Asia/Shanghai -Xms128m -Xmx896m" -Dspring-boot.run.workingDirectory=/srv/projects/order "$@"
