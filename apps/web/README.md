# Web Project

This folder is the self-contained web project for UDchemistry.

## What Is Inside

- `src` - React + TypeScript application code
- `public` - static assets
- `supabase/migrations` - database schema and SQL migrations for the web app
- `docs` - web project planning and supporting documentation
- `.env.example` - required environment variables

## Local Development

```bash
npm install
npm run dev
```

## Vercel Deployment

This project can be deployed directly to Vercel as a Vite app.

Set these environment variables in Vercel:

- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`
- `VITE_APP_URL`

Set `VITE_APP_URL` to your production Vercel domain so generated QR links point to the live site.

## Environment Variables

Copy values from `.env.example` into `.env.local` and provide:

- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`
- `VITE_APP_URL`

## Database Files

The database files that used to live at the repository root are now inside this folder:

```text
apps/web/supabase/migrations
```

This means the web app and its related database setup can be moved or shared as one folder.
