create or replace function public.current_user_institute_id()
returns uuid
language sql
security definer
set search_path = public
stable
as $$
  select institute_id
  from public.profiles
  where id = auth.uid()
    and status = 'active';
$$;

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
    and status = 'active'
  limit 1;
$$;

create or replace function public.admin_create_staff(
  p_full_name text,
  p_username text,
  p_email text,
  p_password text,
  p_institute_id uuid,
  p_phone text default null,
  p_status text default 'active'
)
returns uuid
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_user_id uuid := gen_random_uuid();
  v_now timestamptz := now();
  v_full_name text := trim(p_full_name);
  v_username text := trim(p_username);
  v_email text := lower(trim(p_email));
  v_password text := trim(p_password);
  v_status text := coalesce(trim(p_status), 'active');
begin
  if not public.is_admin() then
    raise exception 'Only admins can manage staff accounts.';
  end if;

  if v_full_name = '' or v_username = '' or v_email = '' then
    raise exception 'Full name, username, and email are required.';
  end if;

  if length(v_password) < 6 then
    raise exception 'Password must be at least 6 characters.';
  end if;

  if p_institute_id is null then
    raise exception 'Institute is required for staff.';
  end if;

  insert into auth.users (
    instance_id,
    id,
    aud,
    role,
    email,
    encrypted_password,
    email_confirmed_at,
    confirmation_token,
    recovery_token,
    email_change_token_new,
    email_change,
    raw_app_meta_data,
    raw_user_meta_data,
    created_at,
    updated_at,
    phone,
    phone_change,
    phone_change_token,
    email_change_token_current,
    email_change_confirm_status,
    reauthentication_token,
    banned_until,
    is_sso_user,
    is_anonymous
  )
  values (
    '00000000-0000-0000-0000-000000000000',
    v_user_id,
    'authenticated',
    'authenticated',
    v_email,
    extensions.crypt(v_password, extensions.gen_salt('bf')),
    v_now,
    '',
    '',
    '',
    '',
    jsonb_build_object('provider', 'email', 'providers', jsonb_build_array('email')),
    jsonb_build_object(
      'role', 'staff',
      'username', v_username,
      'full_name', v_full_name,
      'email_verified', true
    ),
    v_now,
    v_now,
    null,
    '',
    '',
    '',
    0,
    '',
    case when v_status = 'inactive' then v_now + interval '100 years' else null end,
    false,
    false
  );

  insert into auth.identities (
    provider_id,
    user_id,
    identity_data,
    provider,
    created_at,
    updated_at
  )
  values (
    v_user_id::text,
    v_user_id,
    jsonb_build_object(
      'sub', v_user_id::text,
      'email', v_email,
      'email_verified', true,
      'phone_verified', false
    ),
    'email',
    v_now,
    v_now
  );

  insert into public.profiles (
    id,
    full_name,
    email,
    username,
    role,
    institute_id,
    phone,
    status
  )
  values (
    v_user_id,
    v_full_name,
    v_email,
    v_username,
    'staff',
    p_institute_id,
    nullif(trim(coalesce(p_phone, '')), ''),
    v_status
  )
  on conflict (id) do update
  set
    full_name = excluded.full_name,
    email = excluded.email,
    username = excluded.username,
    role = excluded.role,
    institute_id = excluded.institute_id,
    phone = excluded.phone,
    status = excluded.status;

  return v_user_id;
exception
  when unique_violation then
    raise exception 'Email or username is already in use.';
end;
$$;

create or replace function public.admin_update_staff(
  p_staff_id uuid,
  p_full_name text,
  p_username text,
  p_email text,
  p_password text default null,
  p_institute_id uuid default null,
  p_phone text default null,
  p_status text default 'active'
)
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_now timestamptz := now();
  v_full_name text := trim(p_full_name);
  v_username text := trim(p_username);
  v_email text := lower(trim(p_email));
  v_password text := nullif(trim(coalesce(p_password, '')), '');
  v_status text := coalesce(trim(p_status), 'active');
begin
  if not public.is_admin() then
    raise exception 'Only admins can manage staff accounts.';
  end if;

  if not exists (
    select 1
    from public.profiles
    where id = p_staff_id
      and role = 'staff'
  ) then
    raise exception 'Staff account was not found.';
  end if;

  if v_full_name = '' or v_username = '' or v_email = '' then
    raise exception 'Full name, username, and email are required.';
  end if;

  update auth.users
  set
    email = v_email,
    encrypted_password = case
      when v_password is not null then extensions.crypt(v_password, extensions.gen_salt('bf'))
      else encrypted_password
    end,
    raw_user_meta_data = jsonb_build_object(
      'role', 'staff',
      'username', v_username,
      'full_name', v_full_name,
      'email_verified', true
    ),
    email_confirmed_at = coalesce(email_confirmed_at, v_now),
    banned_until = case when v_status = 'inactive' then v_now + interval '100 years' else null end,
    updated_at = v_now
  where id = p_staff_id;

  update auth.identities
  set
    identity_data = jsonb_build_object(
      'sub', p_staff_id::text,
      'email', v_email,
      'email_verified', true,
      'phone_verified', false
    ),
    updated_at = v_now
  where user_id = p_staff_id
    and provider = 'email';

  if not found then
    insert into auth.identities (
      provider_id,
      user_id,
      identity_data,
      provider,
      created_at,
      updated_at
    )
    values (
      p_staff_id::text,
      p_staff_id,
      jsonb_build_object(
        'sub', p_staff_id::text,
        'email', v_email,
        'email_verified', true,
        'phone_verified', false
      ),
      'email',
      v_now,
      v_now
    );
  end if;

  update public.profiles
  set
    full_name = v_full_name,
    email = v_email,
    username = v_username,
    institute_id = p_institute_id,
    phone = nullif(trim(coalesce(p_phone, '')), ''),
    status = v_status
  where id = p_staff_id;
exception
  when unique_violation then
    raise exception 'Email or username is already in use.';
end;
$$;

create or replace function public.admin_delete_staff(p_staff_id uuid)
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
begin
  if not public.is_admin() then
    raise exception 'Only admins can manage staff accounts.';
  end if;

  if not exists (
    select 1
    from public.profiles
    where id = p_staff_id
      and role = 'staff'
  ) then
    raise exception 'Staff account was not found.';
  end if;

  delete from auth.users
  where id = p_staff_id;
end;
$$;

grant execute on function public.admin_create_staff(text, text, text, text, uuid, text, text) to authenticated;
grant execute on function public.admin_update_staff(uuid, text, text, text, text, uuid, text, text) to authenticated;
grant execute on function public.admin_delete_staff(uuid) to authenticated;
