#!/usr/bin/env bash

# this re-runs the setup script in the db container, reseting 
# the db to the original conditions for the purposes of having
# tests always working with the same initial state
echo "re-initializing the contents of the db"
docker exec --user postgres  -w /docker-entrypoint-initdb.d globar_db_1 ./setup-db.sh
