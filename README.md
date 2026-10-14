# MadelMade

The ordering site for MadelMade custom sourdough: a menu, cart and guest or signed-in checkout for customers, and a dashboard where Madeline approves every order, messages customers, tracks bakes and edits the menu.

Java 21 · Spring Boot 4 · Thymeleaf · PostgreSQL. Hosted on Railway.

## Run it locally

You need Java 21, Maven and a local Postgres with a `madelmade` user, password and database.

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Open http://localhost:8080. The starting menu and gallery load on an empty database. In the `dev` profile, sign-in codes are shown on screen instead of emailed, so you can sign in to `/admin` as any admin email.

Tests run against a database named `madelmade_test`: `mvn test`.

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
