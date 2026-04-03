# UDChemistry Web

UDChemistry web application for attendance, payments, QR, classes, and institute management.

## Project Layout

- `src` - React + TypeScript application code
- `public` - static assets
- `supabase/migrations` - database schema and SQL migrations
- `docs` - planning and supporting documentation
- `.env.example` - required environment variables

## Local Development

```bash
npm install
npm run dev
```

## Vercel Deployment

Deploy this repository directly as a Vite app.

Set these environment variables in Vercel:

- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`
- `VITE_APP_URL`

Set `VITE_APP_URL` to your production domain so generated QR links point to the live site.

## Environment Variables

Copy values from `.env.example` into `.env.local` and provide:

- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_PUBLISHABLE_KEY`
- `VITE_APP_URL`
