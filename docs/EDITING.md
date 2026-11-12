# Editing the website

## Everyday changes

Use the dashboard to publish stories, choose the lead, upload pictures, update categories and answer messages. These changes take effect immediately. A source rebuild is needed after changing templates, CSS, JavaScript, bundled assets or Java code.

| Change | File or dashboard screen |
| --- | --- |
| Brand colours, soft shadows and type | The `:root` block near the beginning of `src/main/resources/static/css/site.css`. Adjust `--deep`, `--green`, `--yellow`, `--paper`, `--ink` and `--shadow`. |
| Header and favicon mark | Replace `src/main/resources/static/images/insiders-mark.png` or edit the shared brand fragment and favicon reference in `templates/fragments/site.html`. The current mark is cropped from the supplied green cog. |
| Name, tagline, email and Facebook link | **Admin → Settings**. The Facebook link is initially the share URL supplied by the page owner. |
| Public navigation | Edit the `primaryLinks` fragment in `templates/fragments/site.html`. Desktop and mobile use the same links. The separate contact action is in the header fragment. |
| Homepage layout | `templates/home.html`. The `home` handler in `web/PublicPages.java` chooses the stories, announcements, events and ranking data. |
| Story cards and footer | The `card` and `footer` fragments in `templates/fragments/site.html`. |
| Article body, gallery and player | `templates/article.html`. Article paragraphs are stored as plain text, separated by blank lines. |
| A cover image or its description | Select the cover when editing a post. In **Media**, edit its alternative text. Selected media can be reused across posts. |
| Opening posters, video or captions | `static/images/`, `static/video/` and `edition/opening-edition.json`. The one-time opening edition affects new installations. Use the dashboard for an existing database. |
| Dashboard navigation and forms | `templates/fragments/desk.html` and `templates/admin/`. |
| Search panel, copy controls and delete confirmations | `static/js/site.js`. Primary links and forms work through normal HTML navigation. |
| Photo attribution | `templates/credits.html` and each bundled image’s credit in the opening-edition data. Local font licences sit beside the font files. |

Paths beginning with `templates`, `static` or `edition` in this table are relative to `src/main/resources/`.

## Adding a public page

1. Add a GET handler in the appropriate MVC controller and set the model’s `page` title. Return the template name.
2. Create a template beside `browse.html`. Reuse the shared head, header and footer fragments.
3. Explicitly permit its public GET path in `config/SecurityConfig.java`. Keep editorial endpoints under `/admin`.
4. Add a navigation link if visitors should reach the page from the main menu.
5. Rebuild or run `docker compose up --build -d --wait` and test the new route at a phone and desktop width.

## Adding a feature with stored data

Create the next Flyway migration, add the model and repository, then build its service, controller and template. Keep authorization and ownership checks in the server-side path. Add a test for its public/private boundary.

For example, a future moderated submissions feature can use its own submission table and admin queue, then publish an approved item through the existing publishing service. A future Facebook integration can consume published post information through the public API or an explicit publishing event. Polls, student accounts, comments, reactions and newsletter subscriptions can be added as separate modules when needed.

## Opening-edition credits

The rover illustrations are BRACU Insiders artwork. The original PNG files and visual reference are in the top-level `design/` folder. The supplied ten-second video has local playback, a poster frame and an English caption track. The neumorphism reference guided surface elevation and control styling. Campus photos use credited Wikimedia Commons photographs, with origin and licence links on the public credits page.

Opening-edition stories link to the BRAC University academic calendar and campus, club and residential-semester pages, the European Rover Challenge team register and the respective BIGD event listings. The dates on the site are the newsroom publication dates. Event dates are stored separately.
