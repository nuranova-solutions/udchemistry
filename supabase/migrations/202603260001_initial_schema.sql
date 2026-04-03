create extension if not exists pgcrypto;

create table if not exists public.institutes (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  code text not null unique,
  address text,
  contact_no text,
  status text not null default 'active' check (status in ('active', 'inactive')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  full_name text not null,
  email text not null unique,
  username text not null unique,
  role text not null default 'staff' check (role in ('admin', 'staff')),
  institute_id uuid references public.institutes (id) on delete set null,
  phone text,
  status text not null default 'active' check (status in ('active', 'inactive')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.students (
  id uuid primary key default gen_random_uuid(),
  student_code text unique,
  full_name text not null,
  al_year integer not null,
  institute_id uuid not null references public.institutes (id) on delete cascade,
  qr_code_id uuid unique,
  whatsapp_number text not null,
  qr_link text,
  joined_date date not null default current_date,
  status text not null default 'active' check (status in ('active', 'inactive')),
  created_by uuid references public.profiles (id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.qr_codes (
  id uuid primary key default gen_random_uuid(),
  student_id uuid not null unique references public.students (id) on delete cascade,
  qr_data text not null unique,
  share_token text not null unique,
  qr_link text not null,
  qr_image_path text not null,
  qr_image_url text,
  last_shared_at timestamptz,
  generated_at timestamptz not null default now()
);

create table if not exists public.attendance (
  id uuid primary key default gen_random_uuid(),
  student_id uuid not null references public.students (id) on delete cascade,
  attendance_date date not null,
  status text not null default 'present' check (status in ('present', 'absent', 'late')),
  marked_by uuid references public.profiles (id) on delete set null,
  marked_at timestamptz not null default now(),
  unique (student_id, attendance_date)
);

create table if not exists public.payments (
  id uuid primary key default gen_random_uuid(),
  student_id uuid not null references public.students (id) on delete cascade,
  payment_month integer not null check (payment_month between 1 and 12),
  payment_year integer not null,
  amount numeric(10, 2) not null default 0,
  paid boolean not null default false,
  paid_date date,
  marked_by uuid references public.profiles (id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (student_id, payment_month, payment_year)
);

create index if not exists idx_profiles_institute_id on public.profiles (institute_id);
create index if not exists idx_students_institute_id on public.students (institute_id);
create index if not exists idx_qr_codes_share_token on public.qr_codes (share_token);
create index if not exists idx_attendance_student_date on public.attendance (student_id, attendance_date desc);
create index if not exists idx_payments_student_month_year on public.payments (student_id, payment_year desc, payment_month desc);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists institutes_set_updated_at on public.institutes;
create trigger institutes_set_updated_at
before update on public.institutes
for each row execute procedure public.set_updated_at();

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
before update on public.profiles
for each row execute procedure public.set_updated_at();

drop trigger if exists students_set_updated_at on public.students;
create trigger students_set_updated_at
before update on public.students
for each row execute procedure public.set_updated_at();

drop trigger if exists payments_set_updated_at on public.payments;
create trigger payments_set_updated_at
before update on public.payments
for each row execute procedure public.set_updated_at();

create or replace function public.current_user_institute_id()
returns uuid
language sql
security definer
set search_path = public
stable
as $$
  select institute_id
  from public.profiles
  where id = auth.uid();
$$;

create or replace function public.is_admin()
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select exists (
    select 1
    from public.profiles
    where id = auth.uid()
      and role = 'admin'
      and status = 'active'
  );
$$;

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, full_name, email, username, role, status)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', split_part(new.email, '@', 1)),
    new.email,
    coalesce(new.raw_user_meta_data ->> 'username', split_part(new.email, '@', 1)),
    coalesce(new.raw_user_meta_data ->> 'role', 'staff'),
    'active'
  )
  on conflict (id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute procedure public.handle_new_user();

create or replace function public.get_login_email_by_username(p_username text)
returns text
language sql
security definer
set search_path = public
stable
as $$
  select email
  from public.profiles
  where username = p_username
  limit 1;
$$;

create or replace function public.get_public_qr_by_token(p_share_token text)
returns table (
  qr_image_url text,
  qr_link text
)
language sql
security definer
set search_path = public
stable
as $$
  select q.qr_image_url, q.qr_link
  from public.qr_codes q
  where q.share_token = p_share_token
  limit 1;
$$;

grant execute on function public.get_login_email_by_username(text) to anon, authenticated;
grant execute on function public.get_public_qr_by_token(text) to anon, authenticated;

alter table public.institutes enable row level security;
alter table public.profiles enable row level security;
alter table public.students enable row level security;
alter table public.qr_codes enable row level security;
alter table public.attendance enable row level security;
alter table public.payments enable row level security;

drop policy if exists "institutes_select" on public.institutes;
create policy "institutes_select"
on public.institutes
for select
to authenticated
using (
  public.is_admin()
  or id = public.current_user_institute_id()
);

drop policy if exists "institutes_admin_write" on public.institutes;
create policy "institutes_admin_write"
on public.institutes
for all
to authenticated
using (public.is_admin())
with check (public.is_admin());

drop policy if exists "profiles_self_or_admin_select" on public.profiles;
create policy "profiles_self_or_admin_select"
on public.profiles
for select
to authenticated
using (
  public.is_admin()
  or id = auth.uid()
  or (
    role = 'staff'
    and institute_id = public.current_user_institute_id()
  )
);

drop policy if exists "profiles_self_update" on public.profiles;
create policy "profiles_self_update"
on public.profiles
for update
to authenticated
using (
  public.is_admin()
  or id = auth.uid()
)
with check (
  public.is_admin()
  or id = auth.uid()
);

drop policy if exists "students_select" on public.students;
create policy "students_select"
on public.students
for select
to authenticated
using (
  public.is_admin()
  or institute_id = public.current_user_institute_id()
);

drop policy if exists "students_write" on public.students;
create policy "students_write"
on public.students
for all
to authenticated
using (
  public.is_admin()
  or institute_id = public.current_user_institute_id()
)
with check (
  public.is_admin()
  or institute_id = public.current_user_institute_id()
);

drop policy if exists "qr_codes_select" on public.qr_codes;
create policy "qr_codes_select"
on public.qr_codes
for select
to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.students s
    where s.id = qr_codes.student_id
      and s.institute_id = public.current_user_institute_id()
  )
);

drop policy if exists "qr_codes_write" on public.qr_codes;
create policy "qr_codes_write"
on public.qr_codes
for all
to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.students s
    where s.id = qr_codes.student_id
      and s.institute_id = public.current_user_institute_id()
  )
)
with check (
  public.is_admin()
  or exists (
    select 1
    from public.students s
    where s.id = qr_codes.student_id
      and s.institute_id = public.current_user_institute_id()
  )
);

drop policy if exists "attendance_select" on public.attendance;
create policy "attendance_select"
on public.attendance
for select
to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.students s
    where s.id = attendance.student_id
      and s.institute_id = public.current_user_institute_id()
  )
);

drop policy if exists "attendance_write" on public.attendance;
create policy "attendance_write"
on public.attendance
for all
to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.students s
    where s.id = attendance.student_id
      and s.institute_id = public.current_user_institute_id()
  )
)
with check (
  public.is_admin()
  or exists (
    select 1
    from public.students s
    where s.id = attendance.student_id
      and s.institute_id = public.current_user_institute_id()
  )
);

drop policy if exists "payments_select" on public.payments;
create policy "payments_select"
on public.payments
for select
to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.students s
    where s.id = payments.student_id
      and s.institute_id = public.current_user_institute_id()
  )
);

drop policy if exists "payments_write" on public.payments;
create policy "payments_write"
on public.payments
for all
to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.students s
    where s.id = payments.student_id
      and s.institute_id = public.current_user_institute_id()
  )
)
with check (
  public.is_admin()
  or exists (
    select 1
    from public.students s
    where s.id = payments.student_id
      and s.institute_id = public.current_user_institute_id()
  )
);
