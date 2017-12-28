#!/bin/bash
set -e

while ! psql -U postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='$POSTGRES_USER'" | grep -q 1; do
    echo "Waiting on postgres own initial setup to finish"
    sleep 1
done
sleep 1
while ! pg_isready -U postgres; do
    echo "Waiting on postgres to be ready"
    sleep 1
done

# make sure the fredboat user exists
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -tAc "SELECT 1 FROM pg_roles WHERE rolname='fredboat'" | grep -q 1 || createuser -U "$POSTGRES_USER" fredboat

# make sure the fredboat database exists
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -tc "SELECT 1 FROM pg_database WHERE datname = 'fredboat';" | grep -q 1 || psql -U "$POSTGRES_USER" -c "CREATE DATABASE fredboat WITH OWNER = fredboat;"
# make sure the fredboat datbase is owned by the user fredboat
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -c "ALTER DATABASE fredboat OWNER TO fredboat;"
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -c "GRANT ALL PRIVILEGES ON DATABASE fredboat TO fredboat;"
# make sure HSTORE extension is enabled
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d fredboat -c "CREATE EXTENSION IF NOT EXISTS hstore;"

# make sure the cache database exists
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -tc "SELECT 1 FROM pg_database WHERE datname = 'fredboat_cache';" | grep -q 1 || psql -U "$POSTGRES_USER" -c "CREATE DATABASE fredboat_cache WITH OWNER = fredboat;"
# make sure the cache database is owned by the user fredboat
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -c "ALTER DATABASE fredboat_cache OWNER TO fredboat;"
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -c "GRANT ALL PRIVILEGES ON DATABASE fredboat_cache TO fredboat;"
# make sure HSTORE extension is enabled
psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d fredboat_cache -c "CREATE EXTENSION IF NOT EXISTS hstore;"
