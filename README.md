# UIET Papers Backend (Spring Boot + Supabase)

### Required environment variables
- `DB_URL` – e.g. `jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres`
- `DB_USER` – `postgres`
- `DB_PASSWORD` – your Supabase DB password
- `SUPABASE_URL` – `https://<project-ref>.supabase.co`
- `SUPABASE_SERVICE_KEY` – **service_role** key from Supabase (Settings → API)
- `SUPABASE_BUCKET` – (default: `papers`) – create a public bucket

### Run locally
```bash
# JDK 17 + Maven required
mvn spring-boot:run
```

### Build container
```bash
docker build -t uiet-papers-backend .
docker run -p 8080:8080 --env-file .env uiet-papers-backend
```
