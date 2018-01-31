#!/bin/bash

# We are using this script, because we need to reliably execute a script on each start of the db container,
# to migrate the database for containers with an external volume persisting the database data, where the scripts in
# /docker-entrypoint-initdb.d/ are getting skipped.
# Relevant issue: https://github.com/docker-library/postgres/issues/191

set -e

_term() {
  echo "Caught SIGTERM signal!"
  # sending SIGINT to postgres tells it to do a fast shutdown, which is what we want here due to the default 10 seconds
  # which docker waits until send a SIGKILL which we want to avoid.
  # Relevant docs: https://www.postgresql.org/docs/10/static/server-shutdown.html
  kill -INT "$child" 2>/dev/null
}
trap _term SIGTERM

# running the official postgres entry script in the background
echo "Running entry point"
docker-entrypoint.sh postgres &

# run our own init script
echo "Running init db"
initdb.sh

# Wait on the "docker-entrypoint.sh postgres &" process that we started in the background
child=$!
wait "$child"
