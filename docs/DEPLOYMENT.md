# Deployment and operations

## Hosting the public site

The application needs a Java 21 or Docker host, PostgreSQL and a persistent database volume or managed database. The package contains all templates, fonts and media needed for the first edition.

### Docker on a server

1. Copy the source folder to the server and generate a private `.env` using the launcher or the example in the README. The database service initializes separate cluster-administrator and application credentials.
2. Set the domain’s DNS record to your server. Put an HTTPS reverse proxy in front of the application. The Compose file binds the HTTP service to the server’s loopback interface and keeps PostgreSQL inside the Compose network.
3. Set `COOKIE_SECURE=true` for HTTPS. Configure `FORWARDED_HEADERS=native` and `TRUSTED_PROXY_REGEX` only for the last trusted proxy as seen by the Java container. The regex is a Java pattern matching its exact address, for example `172[.]20[.]0[.]1` if that is the observed Docker gateway. The proxy must replace untrusted incoming forwarding headers. This lets the inbox throttle visitors separately.
4. Run `docker compose up --build -d --wait`. Point the host’s reverse proxy to `127.0.0.1:8080`, or the `HTTP_PORT` you selected. Allow request bodies of at least 26 MB at that proxy for a six-image post.
5. Open `https://YOUR-DOMAIN/health`, then `/admin`. Sign in and confirm the contact address and Facebook link. Check the first public story and upload an image from a phone or desktop.
6. Configure recurring database backups. The uploaded files and private correspondence are included in a PostgreSQL backup.

A host-installed Caddy proxy, for example, can use this Caddyfile after replacing the example hostname with your domain.

```caddyfile
news.example.org {
    reverse_proxy 127.0.0.1:8080
}
```

For an image-based hosting service, build the included Dockerfile and point `DB_URL` at its managed PostgreSQL instance. Supply secrets through the hosting dashboard. The app honours the host’s `PORT` variable. Configure the health-check path as `/health` and use the provider’s required TLS settings for the database connection. When validating a database certificate with a private CA, install that CA and use a verifying JDBC connection configuration.

### Java host with an existing database

Give the application database role ownership of an empty database or migration schema. Set the environment variables in the README, copy `release/bracu-insiders.jar` and run the following under a dedicated service account. Configure your process manager to restart it after failures and at boot.

```bash
java -jar release/bracu-insiders.jar
```

The server applies Flyway migrations on startup. The opening edition runs once per database, under a transaction and a PostgreSQL advisory lock. Keep the current deployment to one application instance, or configure session affinity on the load balancer. Editor sessions, CSRF tokens and submission confirmations use the servlet session. Durable content, inbox receipts, rate limits and popularity counters are stored in PostgreSQL.

## Editor accounts

The first boot creates an editor from `ADMIN_USERNAME` and `ADMIN_PASSWORD`. After initialization, the account’s BCrypt hash is authoritative. The interactive operator command can add a colleague or reset an existing editor. Run it from a terminal with direct access to the application environment.

```bash
docker compose exec app java -jar /app/newsroom.jar --set-editor
```

For a standalone installation, use `java -jar release/bracu-insiders.jar --set-editor` with the same database environment as the running service. The command prompts for the username and asks for the new password twice. Reusing an existing username resets that account. No new password is printed. Once the first admin is created, the bootstrap password can be removed from the running app’s environment. Keep your own password in a password manager.

## Back up and restore

Create a backup with the following commands. Using a file inside the database container keeps the binary archive intact on Windows PowerShell as well as on Unix.

```bash
docker compose exec db pg_dump -U insiders -d insiders -Fc -f /tmp/insiders.dump
docker compose cp db:/tmp/insiders.dump ./insiders.dump
```

For a restore drill, use a **separate, empty target database** owned by the application role. Copy the dump to its database container, then run the following against that target. Substitute the new database name. Start the app against the restored database and verify an article and an attachment.

```bash
docker compose cp ./insiders.dump db:/tmp/restore.dump
docker compose exec db pg_restore -U postgres --no-owner --role=insiders --exit-on-error -d insiders_restore /tmp/restore.dump
```

Protect backups as private records. They include visitor correspondence, uploads and password hashes. Back up the current version before deployment. Retain existing migrations and append a new `V4__...` migration for the next schema change.

## Testing against PostgreSQL

The executable integration suite uses the actual Spring Security chain, MVC controllers, Flyway migrations and PostgreSQL repositories. It creates a new, uniquely named test schema and removes that schema after the run. Give the test login permission to create schemas in a dedicated test database.

Set `TEST_DB_URL` to the JDBC URL of that test database. Set `TEST_DB_USER` and `TEST_DB_PASSWORD`, or use the configured `DB_USER` and `DB_PASSWORD`. With a JDK 21 and Maven installed, run the following.

```bash
mvn clean verify
```

Without `TEST_DB_URL`, Maven skips the PostgreSQL integration suite. Check the **Skipped** count when reproducing the verification.

| Check | Coverage |
| --- | --- |
| Public pages and access control | Front page, article and video pages, event and section pages, public API, admin sign-in, protected routes, missing pages and CSRF enforcement |
| Editorial workflow | Multi-image draft, unpublished-media privacy, publishing, pinning, editing with a stable URL, stale-edit protection, unpublishing and deletion |
| Visitor correspondence | Private PDF, confirmation receipt, editor reply, sender lookup, archive, restore and deletion |
| Site administration | Category create/rename/delete, settings, search, honeypot and rate limit |
| Packaged JAR restart | A published post, its uploaded image, a private PDF, contact message, editor login and receipt all survived a JVM restart. The sender then retrieved a new reply. The application role had no cluster administration permissions. |
| Browser layout | Chromium at 337, 449, 770 and 1,348 CSS pixels. Mobile navigation, contact submission, desktop search and the ten-second video were checked. |

The Java application and database were exercised directly. The Compose file was validated against the Compose schema. An actual Docker-engine launch and public-domain TLS configuration depend on the deployment host.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| First startup exits | Check the database URL and password, the editor username and first password length. Inspect `docker compose logs app db`. |
| Sign-in loops on local HTTP | Use `COOKIE_SECURE=false` for local HTTP, then recreate the app container. Use secure cookies on the HTTPS domain. |
| All visitors share a submission limit | Check the trusted proxy configuration and the client address forwarded by the HTTPS proxy. |
| A story has disappeared from the public site | Check its published status. The public pages and API both exclude drafts. |
| A calendar entry moved out of “Upcoming” | Check the first and last event dates. Completed events remain accessible under **Events → Past events**. |
| An image cannot be deleted | Remove it from its posts first. Uploaded files above the size or pixel limits require resizing. |
| Images or contact messages disappear after replacing a container | Ensure the service is using the same PostgreSQL volume or the same managed database. |
| A setting changed in the seed JSON has no visible effect | Use the dashboard for existing data. The opening-edition import is intentionally one-time. |
