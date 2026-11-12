#!/usr/bin/env sh
set -eu
# The application owns this database, while cluster administration stays with postgres.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'SQL'
\getenv app_password INSIDERS_DB_PASSWORD
CREATE ROLE insiders WITH LOGIN PASSWORD :'app_password' NOSUPERUSER NOCREATEDB NOCREATEROLE;
ALTER DATABASE insiders OWNER TO insiders;
REVOKE CONNECT ON DATABASE insiders FROM PUBLIC;
GRANT CONNECT ON DATABASE insiders TO insiders;
SQL
