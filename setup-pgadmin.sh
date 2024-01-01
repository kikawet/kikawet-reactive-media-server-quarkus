#!/usr/bin/bash
set -xe

CONTAINERNAME=${CONTAINERNAME:-pgadmin}

docker pull dpage/pgadmin4
docker container inspect $CONTAINERNAME &>/dev/null && docker rm $CONTAINERNAME
docker run \
    --name $CONTAINERNAME \
    --volume ./local/dev/pgadmin:/var/lib/pgadmin \
    -p 5050:80 \
    -e 'PGADMIN_DEFAULT_EMAIL=user@domain.com' \
    -e 'PGADMIN_DEFAULT_PASSWORD=SuperSecret' \
    -e 'PGADMIN_CONFIG_ENHANCED_COOKIE_PROTECTION=True' \
    -e 'PGADMIN_CONFIG_LOGIN_BANNER="Authorised users only!"' \
    -e 'PGADMIN_CONFIG_CONSOLE_LOG_LEVEL=10' \
    -d dpage/pgadmin4 \