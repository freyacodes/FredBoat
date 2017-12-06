#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "postgres" -d fredboat -c "CREATE EXTENSION hstore;"
psql -v ON_ERROR_STOP=1 --username "postgres" -d fredboat -c "CREATE EXTENSION pg_trgm;"
