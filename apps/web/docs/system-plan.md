# Chemistry Class QR Attendance and Payment Management System

## 1. Goal

Build a complete institute-aware class management platform with:

- QR-based student attendance
- QR link generation and sharing
- Monthly payment tracking
- Admin and institute staff role-based access
- Real-time dashboards
- Student, staff, institute, and profile management
- Exportable operational reports

Phase 1 focuses on the web application.
Phase 2 adds the mobile scanning app for staff, including 24-hour auto-login.

## 2. Recommended Stack

### Frontend

- React + TypeScript
- Vite for app bootstrapping
- React Router for navigation
- Supabase JS client for auth and database access
- TanStack Query for API state and caching
- React Hook Form + Zod for forms and validation
- TanStack Table for data tables
- Recharts for dashboard graphs
- Framer Motion for animations
- QRCode library for student QR generation
- XLSX + jsPDF + jspdf-autotable for exports

### Mobile (Phase 2)

- React Native + Expo
- Expo Router or React Navigation
- Expo Secure Store for 24-hour session persistence
- Barcode/QR scanner package for camera scanning

### Backend / Data

- Supabase Auth
- Supabase PostgreSQL
- Supabase Storage for QR image files and future asset storage
- Supabase Row Level Security for institute isolation
- Supabase SQL functions and views for dashboard metrics
- Optional Supabase Edge Functions for public QR link resolution and signed asset delivery

### Styling

- Tailwind CSS with project theme tokens
- Reusable component layer for cards, tables, dialogs, forms, and charts

## 3. High-Level Architecture

### Web app

- Single React app
- Route-based layout with sidebar + top header
- Role-aware navigation
- Shared UI system for admin and staff
- Public QR viewer route for download/share access

### Supabase

- Auth handles login and password management
- `profiles` links authenticated users to role and institute
- Business tables store institutes, students, attendance, payments, and QR data
- RLS ensures staff can only access their institute data
- Storage keeps generated QR images instead of storing large image blobs in PostgreSQL

### Mobile app

- Institute staff dashboard
- QR scan-first workflow
- Local secure session with 24-hour expiry
- Reuses the same Supabase project and role model as the web app

### Data flow

1. User logs in with Supabase Auth
2. App fetches the user profile
3. Role and institute scope are stored in app state
4. Every page queries only the data allowed by RLS
5. Dashboard cards/charts are powered by filtered queries or SQL views
6. Student registration generates QR data, QR image, and a shareable QR link
7. QR link can be opened directly or shared through WhatsApp to the student's saved number
8. Attendance scan screen stays QR-only until a valid scan happens

## 4. Roles and Permissions

### Admin

- Full visibility across all institutes
- Manage institutes
- Manage staff
- Manage students
- View and edit attendance and payments
- Access global dashboards and reports
- Manage own profile

### Institute Staff

- Restricted to one institute
- Add and manage students in their own institute
- Mark attendance by QR scan or manual selection
- Mark payments for their own institute students
- View institute-only dashboard and reports
- Manage own profile

## 5. Proposed Database Design

### `profiles`

- `id uuid primary key` references `auth.users.id`
- `full_name text not null`
- `email text not null unique`
- `role text check in ('admin','staff')`
- `institute_id uuid null`
- `phone text null`
- `status text default 'active'`
- `created_at timestamptz default now()`
- `updated_at timestamptz default now()`

Notes:
- Admin users can have `institute_id` null.
- Staff must have an `institute_id`.

### `institutes`

- `id uuid primary key default gen_random_uuid()`
- `name text not null`
- `code text not null unique`
- `address text null`
- `contact_no text null`
- `status text default 'active'`
- `created_at timestamptz default now()`

### `students`

- `id uuid primary key default gen_random_uuid()`
- `student_code text unique`
- `full_name text not null`
- `al_year integer not null`
- `institute_id uuid not null references institutes(id)`
- `qr_code_id uuid unique`
- `whatsapp_number text not null`
- `qr_link text null`
- `joined_date date default current_date`
- `status text default 'active'`
- `created_by uuid references profiles(id)`
- `created_at timestamptz default now()`
- `updated_at timestamptz default now()`

Notes:
- Only the student's WhatsApp number is collected in the initial registration flow.
- `whatsapp_number` should be stored in normalized international format so WhatsApp links work consistently.

### `qr_codes`

- `id uuid primary key default gen_random_uuid()`
- `student_id uuid unique not null references students(id) on delete cascade`
- `qr_data text not null`
- `share_token text unique not null`
- `qr_link text not null`
- `qr_image_path text not null`
- `qr_image_url text null`
- `last_shared_at timestamptz null`
- `generated_at timestamptz default now()`

Notes:
- `qr_data` will store only the `student_id` string as requested.
- `share_token` should be a random token used in the public QR URL so the shared link does not expose the raw student ID.
- `qr_image_path` should point to a file in Supabase Storage instead of saving image binary directly in PostgreSQL.

### `attendance`

- `id uuid primary key default gen_random_uuid()`
- `student_id uuid not null references students(id) on delete cascade`
- `attendance_date date not null`
- `status text default 'present' check in ('present','absent','late')`
- `marked_by uuid references profiles(id)`
- `marked_at timestamptz default now()`
- unique constraint on (`student_id`, `attendance_date`)

Notes:
- The unique constraint prevents duplicate attendance for the same day.

### `payments`

- `id uuid primary key default gen_random_uuid()`
- `student_id uuid not null references students(id) on delete cascade`
- `payment_month integer not null check (payment_month between 1 and 12)`
- `payment_year integer not null`
- `amount numeric(10,2) not null default 0`
- `paid boolean not null default false`
- `paid_date date null`
- `marked_by uuid references profiles(id)`
- `created_at timestamptz default now()`
- unique constraint on (`student_id`, `payment_month`, `payment_year`)

### Optional audit tables for later

- `activity_logs`
- `payment_history`
- `attendance_scan_logs`

## 6. Required Relationships

- One institute has many staff profiles
- One institute has many students
- One student has one QR record
- One student has many attendance records
- One student has many monthly payment records

## 7. Supabase Security Plan

### Auth

- Use email/password sign-in
- On sign-in, load `profiles` row for role and institute scope

### Mobile session policy

- Web uses normal Supabase session persistence
- Mobile app adds a local 24-hour TTL on top of the Supabase session
- On login, store `session_started_at`
- On app startup:
  - if the saved session is less than 24 hours old, auto-login to dashboard
  - if the saved session is older than 24 hours, clear it and send the user to login
- Secure storage must be used for mobile session data

### Row Level Security

RLS must be enabled on every business table.

### Access model

- Admin policy: full access to all rows
- Staff policy: access only rows linked to their `institute_id`
- Self-profile policy: every user can read/update only their own profile
- Public QR access should be limited to the QR viewer route via `share_token`, returning only the minimum fields needed to render/download the QR

### Policy strategy

Create helper SQL functions:

- `is_admin()`
- `current_user_institute_id()`

Then apply policies such as:

- Staff can select students where `students.institute_id = current_user_institute_id()`
- Staff can insert attendance only for students in their institute
- Staff can insert/update payments only for students in their institute
- Staff cannot create or modify institutes or other staff
- Public QR link resolution should happen through a controlled endpoint or Edge Function, not by exposing full student rows

## 8. Core Application Modules

### Authentication

- Login page
- Session persistence
- Role redirect after login
- Protected routes
- Mobile auto-login check with 24-hour expiry in Phase 2

### Dashboard

- Summary cards:
  - Total students
  - Paid this month
  - Unpaid this month
  - Attendance today
  - New registrations
- Charts:
  - Attendance trends by day/week/month
  - Income trends by month
  - New student registrations by month
- Admin sees global data
- Staff sees institute-filtered data

### Institutes

- Admin only
- List institutes
- Add institute
- Edit institute
- View institute summary

### Staff

- Admin only
- List staff
- Add staff user
- Assign institute
- Activate/deactivate staff

### Students

- Admin: all students
- Staff: institute-only students
- Add student
- Edit student
- Search and filter by institute, AL year, status
- Generate and download QR
- Save QR link for each student
- Show QR link column/button in the student table
- Share QR through WhatsApp using the student's saved WhatsApp number

### Attendance

- Daily attendance table
- Filter by date, institute, student, AL year
- Mark attendance manually
- QR scan integration endpoint ready for web and mobile
- Duplicate scan prevention using unique constraint
- Mobile scanning stays available during the 24-hour active session window
- Pre-scan view shows only the scanner/QR area
- Post-scan flow marks attendance immediately, then shows student name, payment actions, and a final `Done` button

### Payments

- Monthly payment table
- Filter by month, year, institute, paid status
- Mark paid/unpaid
- Optional amount support for future flexibility
- Highlight overdue or unpaid students
- Support quick payment marking directly after a successful QR scan

### Reports

- Export students
- Export attendance
- Export payments
- PDF summary reports
- Excel detailed reports
- Filter-driven export scope

### Profile

- Edit name
- Edit phone
- Change password
- Display role and institute

### Public QR Viewer

- Open from shareable QR link
- Display only the QR for viewing/downloading
- Allow download of the QR image
- Keep data exposure minimal

## 9. Web App Route Plan

### Public

- `/login`
- `/qr/:shareToken`

### Protected

- `/dashboard`
- `/institutes`
- `/staff`
- `/students`
- `/attendance`
- `/payments`
- `/reports`
- `/profile`

### Route guard behavior

- Unauthenticated users go to `/login`
- Staff trying to open admin-only pages are redirected to `/dashboard`

## 10. Component Structure

### Layout

- `AppShell`
- `Sidebar`
- `TopBar`
- `PageHeader`
- `ProtectedRoute`
- `RoleGuard`

### Shared UI

- `StatCard`
- `DataTable`
- `SearchFilterBar`
- `FormDialog`
- `EmptyState`
- `ConfirmDialog`
- `StatusBadge`
- `ChartCard`

### Feature components

- `InstituteForm`
- `StaffForm`
- `StudentForm`
- `AttendanceTable`
- `PaymentTable`
- `QrPreviewCard`
- `QrLinkButton`
- `QrShareButton`
- `PublicQrViewer`
- `ScanResultPanel`
- `ExportPanel`
- `ProfileForm`

## 11. Suggested Frontend Folder Structure

```text
src/
  app/
    router/
    providers/
    layouts/
  components/
    ui/
    charts/
    tables/
    forms/
  features/
    auth/
    dashboard/
    institutes/
    staff/
    students/
    attendance/
    payments/
    reports/
    profile/
  lib/
    supabase/
    utils/
    constants/
  hooks/
  pages/
  styles/
  types/
```

## 12. Dashboard Metrics Plan

### Cards

- Total active students
- Students marked present today
- Students who paid for current month
- Students unpaid for current month
- New registrations in current month

### Charts

- Attendance trend:
  - x-axis: date
  - y-axis: present count
- Income trend:
  - x-axis: month
  - y-axis: total payment amount
- Registration trend:
  - x-axis: month
  - y-axis: student count

### Data source approach

- Start with client queries grouped in the app
- Move to SQL views or RPC functions if performance needs improve

## 13. QR Attendance Flow

### QR generation

1. Create student with WhatsApp number
2. Store student row
3. Generate `qr_data = student.id`
4. Generate a random `share_token`
5. Create QR image and save it to Supabase Storage
6. Build an absolute `qr_link = https://app-domain/qr/:shareToken`
7. Insert into `qr_codes`
8. Save the same `qr_link` in `students` if we keep the denormalized column
9. Render downloadable QR image/card with share actions
10. Each student gets one unique QR generated at registration time

### Attendance scan flow

1. Before scan, the screen shows only the QR scanner area
2. Staff or admin scans QR
3. App reads `student_id`
4. Query student scoped by RLS
5. Attempt insert into `attendance`
6. If unique constraint fails, show "already marked today"
7. On success, show the student's name
8. Show payment actions:
   - `Mark as Paid`
   - `Skip`
9. If `Mark as Paid` is chosen, create or update the payment row for the active billing month/year
10. User clicks `Done` to finish the flow and reset for the next scan

### WhatsApp sharing flow

1. User opens the student row or QR preview
2. App reads the student's `whatsapp_number`
3. App builds a WhatsApp deep link with the student's `qr_link`
4. Clicking `Share QR` opens WhatsApp with the target number and pre-filled message
5. System optionally stores `last_shared_at` in `qr_codes`

## 14. Payment Flow

1. User selects month and year
2. User searches or filters student
3. App upserts payment row for that student/month/year
4. Dashboard updates through query invalidation
5. Reports reflect payment status

## 15. Mobile Session Flow

1. User logs in successfully on mobile
2. App stores Supabase session data in secure local storage
3. App stores a local `session_started_at` timestamp
4. On every app launch, the app checks whether the stored session age is within 24 hours
5. Valid session goes straight to dashboard and scanner
6. Expired session is cleared and the user is redirected to login
7. QR scanning and dashboard access work without re-login during the valid 24-hour window

## 16. UX and Visual Direction

### Theme tokens

- Background: `#F3EEFF`
- Header/Text: `#0C162A`
- Primary: `#8C63FF`
- Primary hover: `#A47FFF`
- Secondary button: `#1F2A48`
- Card: `#FFFFFF`
- Divider/Hover: `#E6DEFF`
- Secondary text: `#6B7280`
- Error: `#FF4D4F`
- Success: `#22C55E`

### UI behavior

- Sidebar with active state highlight
- Soft card shadows
- Rounded panels
- Subtle page transition and card pop-in animations
- Button hover lift and color transition
- Responsive two-column to one-column collapse on tablets/mobile

## 17. Reporting Plan

### Export formats

- Excel for full tabular exports
- PDF for printable summaries

### Report filters

- Institute
- Date range
- Month/year
- AL year
- Paid/unpaid
- Attendance status

## 18. Implementation Phases

### Phase 0: Foundation

- Initialize React app
- Configure Tailwind and theme tokens
- Connect Supabase project
- Set up router, auth provider, query provider, layout system
- Define app base URL and WhatsApp sharing format

### Phase 1: Database and Security

- Create tables
- Create indexes and constraints
- Create helper SQL functions
- Enable RLS
- Add all role policies
- Seed admin user and sample data

### Phase 2: Authentication and Shell

- Build login page
- Build protected routes
- Fetch profile on login
- Build role-aware sidebar and topbar
- Define mobile shared auth/session utilities for the future app

### Phase 3: Core CRUD

- Institutes module
- Staff module
- Students module with QR generation, QR link storage, and WhatsApp sharing

### Phase 4: Operational Modules

- Attendance module
- Payments module
- Duplicate prevention and validation
- Public QR viewer page and QR download flow
- Post-scan payment action and `Done` reset flow

### Phase 5: Analytics and Reports

- Dashboard cards and charts
- Reports filters
- Excel and PDF exports

### Phase 6: Profile and Polish

- Profile page
- Password update
- Animation polish
- Responsive fixes
- Error and empty states

### Phase 7: Testing and Release

- Policy testing
- Form validation checks
- CRUD smoke tests
- Role isolation tests
- Production deployment

## 19. Testing Strategy

### Unit

- Form validation
- Data transformers
- Role helpers

### Integration

- Auth flow
- Student creation + QR generation
- QR link generation + download
- WhatsApp share link generation
- Attendance duplicate prevention
- Payment upsert behavior
- Post-scan mark-paid / skip / done flow

### Security

- Staff cannot query other institute data
- Staff cannot update other profiles
- Admin can access all data
- Public QR route exposes only QR-safe information

### Manual QA

- Dashboard accuracy
- Export file correctness
- Mobile responsiveness
- 24-hour session expiry behavior in the mobile app

## 20. Risks and Decisions to Lock Before Build

- Whether student payments use one fixed monthly amount or institute-specific pricing
- Whether attendance allows statuses beyond `present`
- Whether inactive students remain in reports
- Whether staff accounts are created by admin only or also by invite flow
- Whether QR scanning in web phase uses webcam or manual input fallback
- Final production base URL for permanent QR links
- Whether QR images are regenerated on demand or refreshed only on explicit regenerate action

## 21. Recommended Build Order for Us

1. Create the Supabase schema and RLS policies
2. Scaffold the React application and theme system
3. Implement auth and protected routing
4. Build admin and staff dashboards
5. Build institute, staff, and student CRUD
6. Add QR generation, QR link sharing, and attendance flow
7. Add payment management
8. Add reports and exports
9. Add profile management and final polish

## 22. Deliverables for the Web Phase

- Production-ready React web app
- Supabase SQL schema and policies
- Role-based authentication
- Institute-scoped staff access
- Dashboard with charts
- CRUD for institutes, staff, students
- Attendance and payment management
- QR generation, QR link sharing, and QR download for students
- PDF and Excel exports
- Responsive premium UI

## 23. Additional Recommendations

- Normalize student WhatsApp numbers to E.164 format so WhatsApp links work reliably
- Use a private Supabase Storage bucket plus a controlled viewer/download endpoint if QR access should remain revocable later
- Keep `students.qr_link` as a convenience column only; `qr_codes.qr_link` should be the canonical source if both exist
- Add an `updated_at` trigger to all mutable tables from the start
- Prepare a `student_summary` SQL view later for fast dashboard/report joins
- Let the scan screen default payment marking to the current month/year, with an admin/staff override if needed

## 24. Next Step

After plan approval, the next implementation task should be:

- scaffold the React web app
- create the Supabase schema
- wire authentication and role-based route protection
