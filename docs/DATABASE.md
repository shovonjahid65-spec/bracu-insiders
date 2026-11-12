# Database and API

PostgreSQL 16 is the persistent store. The definitive SQL schema is `src/main/resources/db/migration/V1__newsroom.sql`. Flyway runs the migrations in order, then Hibernate validates the mappings. The application starts only after the schema is usable.

| Table | Purpose and relationships |
| --- | --- |
| `admin_accounts` | Unique editor usernames, BCrypt password hashes and administrator role |
| `categories` | Category names, stable slugs, navigation sections and an announcement flag |
| `posts` | Title, caption, body, stable public slug, category reference, draft/published state, pin status, source, video and optional event dates |
| `media` | Public-post image metadata, alternative text, optional bundled path and credit |
| `post_media` | Ordered many-to-many link between an article and its images |
| `media_bytes` | Normalized bytes for uploaded post images, keyed by media ID. Metadata queries can load without the binary payload. |
| `post_reads` | Recorded view count per post, used to rank published stories |
| `messages` | Visitor name, optional email, subject, private body, receipt digest, read state and archive state |
| `message_files` | Up to one private image or PDF per message, stored separately from public media |
| `replies` | Timestamped editorial replies belonging to a message |
| `site_settings` | Publication identity, contact email and Facebook URL |
| `desk_activity` | Timestamped editorial actions. Private-message content is excluded from activity summaries. |
| `rate_windows` | Short-lived, atomic counters shared by application instances |
| `app_state` | Persistent initialization markers and a secret used to hash rate-limit identifiers |

Stories retain the same public URL after a title edit. Posts use a version field and a database lock to detect stale changes. Saving the post, its gallery and newly uploaded image bytes is transactional. Foreign keys protect used media and categories. Deleting a message cascades to its attachment and replies.

Post content is plain text. The application renders it through escaped template fields. Uploaded public images are normalized as JPEG or PNG. Private attachments use a download-only administrator endpoint. A receipt is generated from 24 cryptographically random bytes, and only its SHA-256 digest is persisted.

## Public API

| Endpoint | Result |
| --- | --- |
| `GET /api/posts?page=0&q=Savar&category=opinions` | Published posts, 12 per page, filterable by search text and category slug |
| `GET /api/posts/{slug}` | A published article, its body, public image URLs, source link and video link |
| `GET /media/{id}` | An image attached to a published post |
| `GET /health` | HTTP 200 when the application is ready and PostgreSQL responds. An unavailable service returns 503. |

Listing responses contain `items`, `page`, `totalPages` and `totalItems`. An individual post response includes a summary inside `post` plus `body`, `images`, `source` and `video`. The write interfaces are authenticated MVC forms protected with CSRF tokens.

## Configuration example for a separately installed PostgreSQL server

As a database administrator, create the role and database in psql. The password prompt keeps the secret out of shell arguments and command history.

```sql
CREATE ROLE insiders LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
\password insiders
CREATE DATABASE insiders OWNER insiders;
```

Set `DB_USER=insiders`, `DB_URL=jdbc:postgresql://localhost:5432/insiders` and the matching `DB_PASSWORD` in the application environment. The Compose deployment performs equivalent role initialization on a new volume.
