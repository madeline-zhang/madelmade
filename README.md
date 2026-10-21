# MadelMade

The ordering site for MadelMade custom sourdough: a menu, cart and guest or signed-in checkout for customers, and a dashboard where Madeline approves every order, messages customers, tracks bakes and edits the menu.

Java 21 · Spring Boot 4 · Thymeleaf · PostgreSQL. Hosted on Railway.

## Run it locally

You need Java 21 and Maven. Nothing else: the site brings its own small Postgres.

```
mvn spring-boot:test-run
```

The first run downloads a few things and takes a minute or two. When you see `Started LocalMadelMade`, open http://localhost:8080. Stop it with Ctrl+C.

The starting menu and gallery load the first time. Your test orders and changes are kept in `~/.madelmade-local` between runs; delete that folder to start fresh. Sign-in codes show on screen instead of being emailed, so you can open the dashboard at http://localhost:8080/admin by signing in with madelmadewithlove@gmail.com.

Run the tests with `mvn test`; they also bring their own throwaway Postgres.

## Copy your local menu to the live site (launch day only)

Everything you set up locally (menu, photos, gallery, pages, settings, promo codes) can be copied to the live database in one go. Test orders, test customers and feedback are left behind. Stop the local site first, copy the Postgres **public** connection URL from Railway (Postgres service › Connect), then run:

```
mvn spring-boot:test-run -Dspring-boot.run.arguments="--copy-to-live=postgresql://postgres:...@....proxy.rlwy.net:12345/railway"
```

It refuses to run once the live site has any orders or customers, so it can't overwrite real ones.

## Settings on Railway

| Variable | What it's for |
| --- | --- |
| `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` | The database. Railway's Postgres plugin provides these; reference them from the app service. |
| `APP_BASE_URL` | The public address, e.g. `https://madelmade.com`. Used for links in emails. |
| `ADMIN_EMAILS` | Emails that can always open the dashboard (more can be added in Settings). Defaults to madelmadewithlove@gmail.com. |
| `BREVO_API_KEY` | Brevo key for sending email. Without it nothing is sent; the dashboard's Emails page shows what would have gone out. |
| `MAIL_FROM` | Sender address, verified in Brevo (default `orders@madelmade.com`). |
| `MAIL_REPLY_TO` | Where customer replies go (default madelmadewithlove@gmail.com). |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID`, `..._CLIENT_SECRET` | Turns on "Sign in with Google". Without them only the emailed code is offered. The Google redirect URI is `https://madelmade.com/login/oauth2/code/google`. |

Photos are stored in the database (original plus edited version), so there is no separate image service.

## Where things live

- `src/main/java/com/madelmade/domain` – orders, products, options, customers
- `src/main/java/com/madelmade/service` – pricing, order flow, emails, settings, sign-in codes
- `src/main/java/com/madelmade/web` – customer pages; `web/admin` – the dashboard
- `src/main/resources/templates` – pages, shared components (`components/`), emails (`email/`)
- `src/main/resources/static` – CSS (`tokens.css` holds the colors and fonts), scripts, images
- `src/main/resources/db/migration` – database schema (Flyway)
