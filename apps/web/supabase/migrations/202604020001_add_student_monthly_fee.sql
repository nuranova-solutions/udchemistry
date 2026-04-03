alter table public.students
add column if not exists monthly_fee numeric(12,2) not null default 0 check (monthly_fee >= 0);
