--
-- PostgreSQL database dump
--

\restrict PhWozufpGVXtn9horxXLKTJXnYjVyiDlzxMYgvl9vcfLiAerbZmOAWXLqq0320E

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id uuid NOT NULL,
    user_id uuid,
    action character varying(50) NOT NULL,
    entity_name character varying(50) NOT NULL,
    entity_id character varying(100) NOT NULL,
    old_values jsonb,
    new_values jsonb,
    ip_address character varying(45),
    user_agent text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: favorites; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.favorites (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    post_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: floors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.floors (
    id uuid NOT NULL,
    house_id uuid NOT NULL,
    floor_number integer NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(500)
);


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: houses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.houses (
    id uuid NOT NULL,
    landlord_id uuid NOT NULL,
    name character varying(150) NOT NULL,
    address_street character varying(255) NOT NULL,
    ward character varying(100) NOT NULL,
    district character varying(100) NOT NULL,
    city character varying(100) NOT NULL,
    latitude numeric(10,7),
    longitude numeric(10,7),
    description text,
    total_floors integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    title character varying(150) NOT NULL,
    content text NOT NULL,
    type character varying(30) NOT NULL,
    reference_id uuid,
    is_read boolean DEFAULT false NOT NULL,
    read_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    target_url character varying(500),
    CONSTRAINT ck_notifications_type CHECK (((type)::text = ANY ((ARRAY['SYSTEM'::character varying, 'INVOICE'::character varying, 'POST_APPROVAL'::character varying, 'INTEREST_REQUEST'::character varying, 'CONTACT_CARD'::character varying, 'GENERAL'::character varying])::text[])))
);


--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(64) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: posts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.posts (
    id uuid NOT NULL,
    landlord_id uuid NOT NULL,
    room_id uuid NOT NULL,
    title character varying(200) NOT NULL,
    description text NOT NULL,
    rental_price numeric(15,2),
    deposit_amount numeric(15,2) NOT NULL,
    available_from date NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    reject_reason text,
    view_count bigint DEFAULT 0 NOT NULL,
    approved_by uuid,
    approved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_posts_deposit_amount CHECK ((deposit_amount >= (0)::numeric)),
    CONSTRAINT ck_posts_rental_price CHECK ((rental_price >= (0)::numeric)),
    CONSTRAINT ck_posts_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'HIDDEN'::character varying])::text[])))
);


--
-- Name: processed_rental_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.processed_rental_events (
    event_id uuid NOT NULL,
    room_id uuid NOT NULL,
    event_sequence bigint NOT NULL,
    event_type character varying(40) NOT NULL,
    processed_at timestamp with time zone NOT NULL
);


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    family_id uuid NOT NULL,
    token_hash character varying(64) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone,
    revoked_at timestamp with time zone
);


--
-- Name: room_images; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.room_images (
    id uuid NOT NULL,
    room_id uuid NOT NULL,
    image_url character varying(1000) NOT NULL,
    is_primary boolean DEFAULT false NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    uploaded_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: rooms; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rooms (
    id uuid NOT NULL,
    floor_id uuid NOT NULL,
    room_number character varying(50) NOT NULL,
    area numeric(10,2),
    base_price numeric(15,2),
    max_tenants integer NOT NULL,
    status character varying(20) NOT NULL,
    has_private_bathroom boolean DEFAULT false NOT NULL,
    has_air_conditioner boolean DEFAULT false NOT NULL,
    has_water_heater boolean DEFAULT false NOT NULL,
    has_balcony boolean DEFAULT false NOT NULL,
    amenities_description text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_rented boolean DEFAULT false NOT NULL,
    CONSTRAINT ck_rooms_status CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'OCCUPIED'::character varying, 'RESERVED'::character varying, 'MAINTENANCE'::character varying])::text[])))
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    username character varying(50) NOT NULL,
    email character varying(254) NOT NULL,
    password_hash character varying(100) NOT NULL,
    full_name character varying(100) NOT NULL,
    phone_number character varying(20) NOT NULL,
    avatar_url character varying(500),
    role character varying(20) NOT NULL,
    status character varying(30) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    password_changed_at timestamp with time zone,
    zalo character varying(100),
    CONSTRAINT ck_users_role CHECK (((role)::text = ANY ((ARRAY['TENANT'::character varying, 'LANDLORD'::character varying, 'ADMIN'::character varying])::text[]))),
    CONSTRAINT ck_users_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'LOCKED'::character varying, 'PENDING_VERIFICATION'::character varying])::text[])))
);


--
-- Name: violation_reports; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.violation_reports (
    id uuid NOT NULL,
    reporter_id uuid NOT NULL,
    target_type character varying(20) NOT NULL,
    target_id uuid NOT NULL,
    reason character varying(200) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    resolution text,
    reviewed_by uuid,
    reviewed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.audit_logs (id, user_id, action, entity_name, entity_id, old_values, new_values, ip_address, user_agent, created_at) FROM stdin;
\.


--
-- Data for Name: favorites; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.favorites (id, tenant_id, post_id, created_at) FROM stdin;
\.


--
-- Data for Name: floors; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.floors (id, house_id, floor_number, name, description) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create users	SQL	V1__create_users.sql	421907070	postgres	2026-10-17 17:27:39.423738	21	t
2	2	create admin user management	SQL	V2__create_admin_user_management.sql	1140015169	postgres	2026-10-17 17:27:39.497609	59	t
3	3	create posts	SQL	V3__create_posts.sql	623165572	postgres	2026-10-17 17:27:39.590999	46	t
4	4	add landlord room management	SQL	V4__add_landlord_room_management.sql	-193728143	postgres	2026-10-17 17:27:39.668744	35	t
5	5	create favorites	SQL	V5__create_favorites.sql	1077615958	postgres	2026-10-17 17:27:39.730699	17	t
6	6	track rental events	SQL	V6__track_rental_events.sql	-504923066	postgres	2026-10-17 17:27:39.762872	13	t
7	7	create password reset tokens	SQL	V7__create_password_reset_tokens.sql	-2073493631	postgres	2026-10-26 08:30:02.598659	146	t
8	8	align password reset token hash type	SQL	V8__align_password_reset_token_hash_type.sql	383352523	postgres	2026-10-26 08:32:05.079324	66	t
9	9	create refresh tokens	SQL	V9__create_refresh_tokens.sql	-997488087	postgres	2026-10-27 09:49:51.366728	200	t
10	10	create violation reports	SQL	V10__create_violation_reports.sql	1794776921	postgres	2026-10-27 12:37:33.784383	222	t
11	11	add zalo contact	SQL	V11__add_zalo_contact.sql	1461260990	postgres	2026-10-27 12:37:34.221349	70	t
12	12	add notification target url	SQL	V12__add_notification_target_url.sql	-1937033097	postgres	2026-10-31 05:05:33.261342	70	t
\.


--
-- Data for Name: houses; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.houses (id, landlord_id, name, address_street, ward, district, city, latitude, longitude, description, total_floors, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.notifications (id, user_id, title, content, type, reference_id, is_read, read_at, created_at, target_url) FROM stdin;
ade7c8cd-ca3c-428e-958d-fab33c061992	06083eab-0de2-4d9c-abae-64b66ab3e1f5	Tài khoản đã bị khóa	Tài khoản của bạn đã bị quản trị viên khóa.	SYSTEM	06083eab-0de2-4d9c-abae-64b66ab3e1f5	f	\N	2026-10-31 05:44:28.229236+00	/profile
114616e0-dc32-4653-b576-e85911f8976d	06083eab-0de2-4d9c-abae-64b66ab3e1f5	Tài khoản đã được mở khóa	Tài khoản của bạn đã được quản trị viên mở khóa.	SYSTEM	06083eab-0de2-4d9c-abae-64b66ab3e1f5	f	\N	2026-10-31 05:44:54.082675+00	/profile
\.


--
-- Data for Name: password_reset_tokens; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.password_reset_tokens (id, user_id, token_hash, expires_at, used_at, created_at) FROM stdin;
\.


--
-- Data for Name: posts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.posts (id, landlord_id, room_id, title, description, rental_price, deposit_amount, available_from, status, reject_reason, view_count, approved_by, approved_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: processed_rental_events; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.processed_rental_events (event_id, room_id, event_sequence, event_type, processed_at) FROM stdin;
\.


--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.refresh_tokens (id, user_id, family_id, token_hash, created_at, expires_at, used_at, revoked_at) FROM stdin;
8524604f-5981-4a72-beb8-b366eb56d686	00000000-0000-0000-0000-000000000003	7787d24e-2863-4121-8c60-aaf40feedf79	7685578e2994ceab3b5071371edb33cffacdee3c81f5967f01b6034109392685	2026-11-01 16:52:47.690941+00	2026-12-01 16:52:47.601814+00	\N	\N
6a423c45-6748-4544-9a8b-fff6ef61b381	00000000-0000-0000-0000-000000000003	7787d24e-2863-4121-8c60-aaf40feedf79	a03612613883925450e6de7d0bce2d5b15b414e0bc2686c80d4b2bbafd5e5aad	2026-11-01 16:47:25.61154+00	2026-12-01 16:47:25.607781+00	2026-11-01 16:52:47.534201+00	\N
\.


--
-- Data for Name: room_images; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.room_images (id, room_id, image_url, is_primary, display_order, uploaded_at) FROM stdin;
\.


--
-- Data for Name: rooms; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.rooms (id, floor_id, room_number, area, base_price, max_tenants, status, has_private_bathroom, has_air_conditioner, has_water_heater, has_balcony, amenities_description, created_at, updated_at, is_rented) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, username, email, password_hash, full_name, phone_number, avatar_url, role, status, created_at, updated_at, password_changed_at, zalo) FROM stdin;
46d0de5b-14e4-4637-a541-eeea3b52759b	sidz	si9x992005@gmail.com	$2a$10$vxhyzujj4SVa6ooT2XgoU.6FS6QxiD20nKzgTp8MKBhv05gQQn3TG	Sĩ Đỗ	0987654321	\N	TENANT	ACTIVE	2026-10-27 08:03:47.085504+00	2026-10-27 08:03:47.085598+00	\N	\N
06083eab-0de2-4d9c-abae-64b66ab3e1f5	e2e.tenant.20261027	e2e.tenant.20261027@roomix.local	$2a$10$BCFr50jaWHxB0MSNt/7My.5BXY/KyT11PFCyHJfDMz8dPz7SptNAe	E2E Tenant	+84912345678	\N	TENANT	ACTIVE	2026-10-27 08:12:36.959674+00	2026-10-31 05:44:54.085306+00	\N	\N
00000000-0000-0000-0000-000000000001	admin.seed	admin.seed@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Quản trị viên Roomix	+84900000001	\N	ADMIN	ACTIVE	2026-10-27 08:08:35.506384+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000002	landlord.seed	landlord.seed@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Nguyễn Minh Hoàng	+84900000002	\N	LANDLORD	ACTIVE	2026-10-27 08:08:35.506384+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	0900000002
00000000-0000-0000-0000-000000000003	tenant.seed	tenant.seed@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Trần Gia Hân	+84900000003	\N	TENANT	ACTIVE	2026-10-27 08:08:35.506384+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000004	landlord.demo.4	landlord.demo.4@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Lê Quốc Bảo	+849100000004	\N	LANDLORD	ACTIVE	2026-04-23 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	0910000004
00000000-0000-0000-0000-000000000005	landlord.demo.5	landlord.demo.5@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Phạm Thùy Dương	+849100000005	\N	LANDLORD	ACTIVE	2026-04-26 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	0910000005
00000000-0000-0000-0000-000000000006	landlord.demo.6	landlord.demo.6@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Võ Đức Anh	+849100000006	\N	LANDLORD	ACTIVE	2026-04-29 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	0910000006
00000000-0000-0000-0000-000000000007	landlord.demo.7	landlord.demo.7@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Đặng Khánh Linh	+849100000007	\N	LANDLORD	PENDING_VERIFICATION	2026-05-02 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	0910000007
00000000-0000-0000-0000-000000000008	tenant.demo.8	tenant.demo.8@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Nguyễn Tuấn Kiệt	+849100000008	\N	TENANT	ACTIVE	2026-05-05 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000009	tenant.demo.9	tenant.demo.9@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Trương Mỹ Duyên	+849100000009	\N	TENANT	ACTIVE	2026-05-08 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000010	tenant.demo.10	tenant.demo.10@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Phan Hoàng Nam	+849100000010	\N	TENANT	ACTIVE	2026-05-11 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000011	tenant.demo.11	tenant.demo.11@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Bùi Thanh Trúc	+849100000011	\N	TENANT	ACTIVE	2026-05-14 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000012	tenant.demo.12	tenant.demo.12@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Lý Minh Khang	+849100000012	\N	TENANT	ACTIVE	2026-05-17 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000013	tenant.demo.13	tenant.demo.13@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Đỗ Ngọc Mai	+849100000013	\N	TENANT	ACTIVE	2026-05-20 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000014	tenant.demo.14	tenant.demo.14@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Hồ Anh Quân	+849100000014	\N	TENANT	ACTIVE	2026-05-23 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000015	tenant.demo.15	tenant.demo.15@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Dương Bảo Châu	+849100000015	\N	TENANT	ACTIVE	2026-05-26 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000016	tenant.demo.16	tenant.demo.16@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Mai Nhật Long	+849100000016	\N	TENANT	ACTIVE	2026-05-29 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000017	tenant.demo.17	tenant.demo.17@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Tạ Thu Hà	+849100000017	\N	TENANT	ACTIVE	2026-06-01 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000019	tenant.demo.19	tenant.demo.19@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Ngô Phương Thảo	+849100000019	\N	TENANT	LOCKED	2026-06-07 16:49:44.177953+00	2026-10-28 16:52:51.723276+00	2026-10-28 16:52:51.723276+00	\N
00000000-0000-0000-0000-000000000018	tenant.demo.18	tenant.demo.18@roomix.local	$2a$10$JC4Vzmxj2ganLrsR6loIoePDD1Eklrn6EOZ7eZ.vb4a7tIRV1eqAS	Vũ Quốc Huy	+849100000018	\N	TENANT	ACTIVE	2026-06-04 16:49:44.177953+00	2026-10-31 06:55:50.698089+00	2026-10-28 16:52:51.723276+00	\N
\.


--
-- Data for Name: violation_reports; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.violation_reports (id, reporter_id, target_type, target_id, reason, description, status, resolution, reviewed_by, reviewed_at, created_at, updated_at) FROM stdin;
\.


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: favorites favorites_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT favorites_pkey PRIMARY KEY (id);


--
-- Name: floors floors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.floors
    ADD CONSTRAINT floors_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: houses houses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.houses
    ADD CONSTRAINT houses_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_token_hash_key UNIQUE (token_hash);


--
-- Name: posts posts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_pkey PRIMARY KEY (id);


--
-- Name: processed_rental_events processed_rental_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.processed_rental_events
    ADD CONSTRAINT processed_rental_events_pkey PRIMARY KEY (event_id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash);


--
-- Name: room_images room_images_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.room_images
    ADD CONSTRAINT room_images_pkey PRIMARY KEY (id);


--
-- Name: rooms rooms_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rooms
    ADD CONSTRAINT rooms_pkey PRIMARY KEY (id);


--
-- Name: favorites uk_favorites_tenant_post; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT uk_favorites_tenant_post UNIQUE (tenant_id, post_id);


--
-- Name: floors uk_floors_house_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.floors
    ADD CONSTRAINT uk_floors_house_number UNIQUE (house_id, floor_number);


--
-- Name: rooms uk_rooms_floor_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rooms
    ADD CONSTRAINT uk_rooms_floor_number UNIQUE (floor_id, room_number);


--
-- Name: users uk_users_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_users_email UNIQUE (email);


--
-- Name: users uk_users_username; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_users_username UNIQUE (username);


--
-- Name: processed_rental_events uq_processed_room_sequence; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.processed_rental_events
    ADD CONSTRAINT uq_processed_room_sequence UNIQUE (room_id, event_sequence);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: violation_reports violation_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.violation_reports
    ADD CONSTRAINT violation_reports_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_audit_logs_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_created_at ON public.audit_logs USING btree (created_at DESC);


--
-- Name: idx_audit_logs_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_entity ON public.audit_logs USING btree (entity_name, entity_id);


--
-- Name: idx_audit_logs_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_logs_user_id ON public.audit_logs USING btree (user_id);


--
-- Name: idx_favorites_post_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_favorites_post_id ON public.favorites USING btree (post_id);


--
-- Name: idx_favorites_tenant_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_favorites_tenant_created ON public.favorites USING btree (tenant_id, created_at DESC);


--
-- Name: idx_floors_house_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_floors_house_id ON public.floors USING btree (house_id);


--
-- Name: idx_houses_landlord_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_houses_landlord_id ON public.houses USING btree (landlord_id);


--
-- Name: idx_notifications_user_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_user_created ON public.notifications USING btree (user_id, created_at DESC);


--
-- Name: idx_password_reset_tokens_user_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_password_reset_tokens_user_active ON public.password_reset_tokens USING btree (user_id, expires_at DESC) WHERE (used_at IS NULL);


--
-- Name: idx_posts_landlord_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posts_landlord_id ON public.posts USING btree (landlord_id);


--
-- Name: idx_posts_room_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posts_room_id ON public.posts USING btree (room_id);


--
-- Name: idx_posts_status_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posts_status_created ON public.posts USING btree (status, created_at DESC);


--
-- Name: idx_refresh_tokens_expiry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_expiry ON public.refresh_tokens USING btree (expires_at);


--
-- Name: idx_refresh_tokens_family; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_family ON public.refresh_tokens USING btree (family_id);


--
-- Name: idx_refresh_tokens_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_user ON public.refresh_tokens USING btree (user_id, created_at DESC);


--
-- Name: idx_room_images_room_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_room_images_room_order ON public.room_images USING btree (room_id, display_order);


--
-- Name: idx_rooms_floor_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_rooms_floor_id ON public.rooms USING btree (floor_id);


--
-- Name: idx_violation_reports_reporter_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_violation_reports_reporter_created ON public.violation_reports USING btree (reporter_id, created_at DESC);


--
-- Name: idx_violation_reports_status_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_violation_reports_status_created ON public.violation_reports USING btree (status, created_at DESC);


--
-- Name: ix_processed_rental_room_sequence; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_processed_rental_room_sequence ON public.processed_rental_events USING btree (room_id, event_sequence DESC);


--
-- Name: audit_logs audit_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: favorites favorites_post_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT favorites_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id) ON DELETE CASCADE;


--
-- Name: favorites favorites_tenant_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT favorites_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: floors floors_house_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.floors
    ADD CONSTRAINT floors_house_id_fkey FOREIGN KEY (house_id) REFERENCES public.houses(id) ON DELETE CASCADE;


--
-- Name: houses houses_landlord_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.houses
    ADD CONSTRAINT houses_landlord_id_fkey FOREIGN KEY (landlord_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: password_reset_tokens password_reset_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: posts posts_approved_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_approved_by_fkey FOREIGN KEY (approved_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: posts posts_landlord_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_landlord_id_fkey FOREIGN KEY (landlord_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: posts posts_room_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_room_id_fkey FOREIGN KEY (room_id) REFERENCES public.rooms(id) ON DELETE RESTRICT;


--
-- Name: processed_rental_events processed_rental_events_room_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.processed_rental_events
    ADD CONSTRAINT processed_rental_events_room_id_fkey FOREIGN KEY (room_id) REFERENCES public.rooms(id);


--
-- Name: refresh_tokens refresh_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: room_images room_images_room_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.room_images
    ADD CONSTRAINT room_images_room_id_fkey FOREIGN KEY (room_id) REFERENCES public.rooms(id) ON DELETE CASCADE;


--
-- Name: rooms rooms_floor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rooms
    ADD CONSTRAINT rooms_floor_id_fkey FOREIGN KEY (floor_id) REFERENCES public.floors(id) ON DELETE RESTRICT;


--
-- Name: violation_reports violation_reports_reporter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.violation_reports
    ADD CONSTRAINT violation_reports_reporter_id_fkey FOREIGN KEY (reporter_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: violation_reports violation_reports_reviewed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.violation_reports
    ADD CONSTRAINT violation_reports_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--

\unrestrict PhWozufpGVXtn9horxXLKTJXnYjVyiDlzxMYgvl9vcfLiAerbZmOAWXLqq0320E

