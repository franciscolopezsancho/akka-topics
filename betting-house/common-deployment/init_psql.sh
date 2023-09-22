#!/bin/bash
set -e

# Copy .sql files to PostgreSQL data directory
cp /init-scripts/*.sql /docker-entrypoint-initdb.d/

# Start PostgreSQL
exec gosu postgres /usr/local/bin/docker-entrypoint.sh postgres
