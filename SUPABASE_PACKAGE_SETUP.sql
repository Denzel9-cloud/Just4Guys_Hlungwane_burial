-- Run once in Supabase SQL Editor. Package selections are separate from insured policies.
create table if not exists public.package_applications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  option_number integer not null check (option_number between 1 and 4),
  status text not null default 'pending' check (status in ('pending','approved','rejected')),
  created_at timestamptz not null default now(),
  reviewed_at timestamptz,
  reviewed_by uuid references auth.users(id)
);
alter table public.package_applications enable row level security;
create policy "Customers can view own applications" on public.package_applications
for select to authenticated using (user_id = (select auth.uid()));
create policy "Customers can submit own applications" on public.package_applications
for insert to authenticated with check (user_id = (select auth.uid()) and status = 'pending' and reviewed_by is null and reviewed_at is null);
-- Do not allow customers to update status. Approvals must be performed through
-- a server-side staff-authorized endpoint with a verified staff role and audit log.
