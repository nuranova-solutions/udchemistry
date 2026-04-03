create table if not exists public.classes (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  institute_id uuid not null references public.institutes (id) on delete cascade,
  al_year integer not null,
  monthly_fee numeric(12, 2) not null default 0 check (monthly_fee >= 0),
  class_type text not null default 'general' check (class_type in ('general', 'extra')),
  weekday text not null check (
    weekday in ('monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday')
  ),
  start_time time not null,
  end_time time not null,
  week_of_month integer check (week_of_month between 1 and 5),
  active_from date not null default current_date,
  active_until date,
  status text not null default 'active' check (status in ('active', 'inactive')),
  notes text,
  created_by uuid references public.profiles (id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (active_until is null or active_until >= active_from),
  check (
    (class_type = 'general' and week_of_month is null)
    or (class_type = 'extra' and week_of_month between 1 and 5)
  )
);

create index if not exists idx_classes_institute_id on public.classes (institute_id);
create index if not exists idx_classes_year_type on public.classes (al_year, class_type);
create index if not exists idx_classes_weekday_status on public.classes (weekday, status);

drop trigger if exists classes_set_updated_at on public.classes;
create trigger classes_set_updated_at
before update on public.classes
for each row execute procedure public.set_updated_at();

alter table public.classes enable row level security;

drop policy if exists "classes_select" on public.classes;
create policy "classes_select"
on public.classes
for select
to authenticated
using (
  public.is_admin()
  or institute_id = public.current_user_institute_id()
);

drop policy if exists "classes_write" on public.classes;
create policy "classes_write"
on public.classes
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
