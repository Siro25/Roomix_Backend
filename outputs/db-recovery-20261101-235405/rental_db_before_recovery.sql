--
-- PostgreSQL database dump
--

\restrict EdC0HiR8VNa1T7K9hB1NcuOnTAwxim986I532DzdRvStxEgpmLy6XewI0GSRxlR

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
-- Name: active_tenancies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.active_tenancies (
    id uuid NOT NULL,
    room_id uuid NOT NULL,
    contract_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    check_in_date date NOT NULL,
    assigned_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    id uuid NOT NULL,
    user_id uuid,
    action character varying(60) NOT NULL,
    entity_name character varying(50) NOT NULL,
    entity_id character varying(100) NOT NULL,
    old_values jsonb DEFAULT '{}'::jsonb NOT NULL,
    new_values jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: contact_cards; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contact_cards (
    id uuid NOT NULL,
    rental_request_id uuid NOT NULL,
    landlord_id uuid NOT NULL,
    landlord_name character varying(100) NOT NULL,
    landlord_phone character varying(20) NOT NULL,
    landlord_zalo character varying(100) NOT NULL,
    landlord_avatar_url character varying(500),
    note character varying(1000),
    issued_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: contracts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contracts (
    id uuid NOT NULL,
    contract_code character varying(50) NOT NULL,
    rental_request_id uuid NOT NULL,
    room_id uuid NOT NULL,
    landlord_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    actual_return_date date,
    monthly_rent numeric(15,2) NOT NULL,
    deposit_amount numeric(15,2) NOT NULL,
    electricity_unit_price numeric(15,2) NOT NULL,
    water_unit_price numeric(15,2) NOT NULL,
    initial_electricity numeric(15,2) NOT NULL,
    initial_water numeric(15,2) NOT NULL,
    status character varying(20) NOT NULL,
    terms_and_conditions character varying(5000),
    cancellation_reason character varying(2000),
    deposit_confirmed_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deposit_deadline timestamp with time zone NOT NULL,
    CONSTRAINT ck_contract_amounts CHECK (((monthly_rent > (0)::numeric) AND (deposit_amount >= (0)::numeric) AND (electricity_unit_price >= (0)::numeric) AND (water_unit_price >= (0)::numeric) AND (initial_electricity >= (0)::numeric) AND (initial_water >= (0)::numeric))),
    CONSTRAINT ck_contract_dates CHECK ((end_date > start_date)),
    CONSTRAINT ck_contract_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying, 'ENDED'::character varying, 'CANCELLED'::character varying])::text[])))
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
-- Name: invoice_corrections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoice_corrections (
    id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    revision integer NOT NULL,
    corrected_by uuid NOT NULL,
    reason character varying(2000) NOT NULL,
    old_values jsonb NOT NULL,
    new_values jsonb NOT NULL,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT invoice_corrections_reason_check CHECK ((length(TRIM(BOTH FROM reason)) > 0)),
    CONSTRAINT invoice_corrections_revision_check CHECK ((revision > 0))
);


--
-- Name: invoice_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoice_items (
    id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    item_name character varying(100) NOT NULL,
    item_type character varying(20) NOT NULL,
    quantity numeric(15,2) NOT NULL,
    unit_price numeric(15,2) NOT NULL,
    total_price numeric(19,2) NOT NULL,
    CONSTRAINT ck_item_amounts CHECK (((quantity >= (0)::numeric) AND (unit_price >= (0)::numeric) AND (total_price = round((quantity * unit_price), 2))))
);


--
-- Name: invoice_reminders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoice_reminders (
    id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    landlord_id uuid NOT NULL,
    reminder_date date NOT NULL,
    outstanding_amount numeric(19,2) NOT NULL,
    content character varying(500) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT invoice_reminders_outstanding_amount_check CHECK ((outstanding_amount > (0)::numeric))
);


--
-- Name: invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoices (
    id uuid NOT NULL,
    invoice_code character varying(50) NOT NULL,
    contract_id uuid NOT NULL,
    meter_reading_id uuid NOT NULL,
    billing_month character varying(7) NOT NULL,
    due_date date NOT NULL,
    total_amount numeric(19,2) NOT NULL,
    paid_amount numeric(19,2) NOT NULL,
    status character varying(20) NOT NULL,
    is_disputed boolean NOT NULL,
    dispute_reason character varying(2000),
    dispute_resolution character varying(2000),
    confirmed_at timestamp with time zone,
    payment_date timestamp with time zone,
    payment_method character varying(30),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    revision integer DEFAULT 0 NOT NULL,
    CONSTRAINT ck_invoice_amounts CHECK (((total_amount >= (0)::numeric) AND (paid_amount >= (0)::numeric) AND (paid_amount <= total_amount))),
    CONSTRAINT ck_invoice_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'PAID'::character varying])::text[]))),
    CONSTRAINT invoices_revision_check CHECK ((revision >= 0))
);


--
-- Name: meter_readings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meter_readings (
    id uuid NOT NULL,
    contract_id uuid NOT NULL,
    room_id uuid NOT NULL,
    billing_month character varying(7) NOT NULL,
    old_electricity_val numeric(15,2) NOT NULL,
    new_electricity_val numeric(15,2) NOT NULL,
    electricity_usage numeric(15,2) NOT NULL,
    old_water_val numeric(15,2) NOT NULL,
    new_water_val numeric(15,2) NOT NULL,
    water_usage numeric(15,2) NOT NULL,
    recorded_by uuid NOT NULL,
    reading_date date NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_meter_values CHECK (((old_electricity_val >= (0)::numeric) AND (new_electricity_val >= old_electricity_val) AND (old_water_val >= (0)::numeric) AND (new_water_val >= old_water_val) AND (electricity_usage = (new_electricity_val - old_electricity_val)) AND (water_usage = (new_water_val - old_water_val))))
);


--
-- Name: rental_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rental_requests (
    id uuid NOT NULL,
    room_id uuid NOT NULL,
    post_id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    landlord_id uuid NOT NULL,
    note character varying(2000),
    status character varying(20) NOT NULL,
    reject_reason character varying(2000),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_request_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying, 'REJECTED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: room_booking_locks; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.room_booking_locks (
    room_id uuid NOT NULL
);


--
-- Name: tenancy_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tenancy_events (
    sequence bigint NOT NULL,
    event_id uuid NOT NULL,
    event_type character varying(40) NOT NULL,
    room_id uuid NOT NULL,
    tenancy_id uuid NOT NULL,
    partition_key character varying(36) NOT NULL,
    topic character varying(100) NOT NULL,
    occurred_at timestamp with time zone NOT NULL,
    published_at timestamp with time zone,
    attempt_count integer DEFAULT 0 NOT NULL,
    next_attempt_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_error character varying(1000),
    CONSTRAINT ck_event_partition_key CHECK (((partition_key)::text = (room_id)::text)),
    CONSTRAINT tenancy_events_event_type_check CHECK (((event_type)::text = ANY ((ARRAY['TENANCY_ACTIVATED'::character varying, 'TENANCY_ENDED'::character varying])::text[])))
);


--
-- Name: tenancy_events_sequence_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tenancy_events ALTER COLUMN sequence ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tenancy_events_sequence_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tenancy_settlements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tenancy_settlements (
    id uuid NOT NULL,
    contract_id uuid NOT NULL,
    actual_return_date date NOT NULL,
    last_billed_month character varying(7),
    unbilled_months integer NOT NULL,
    previous_electricity numeric(15,2) NOT NULL,
    final_electricity numeric(15,2) NOT NULL,
    previous_water numeric(15,2) NOT NULL,
    final_water numeric(15,2) NOT NULL,
    monthly_rent numeric(15,2) NOT NULL,
    electricity_unit_price numeric(15,2) NOT NULL,
    water_unit_price numeric(15,2) NOT NULL,
    invoice_debt numeric(19,2) NOT NULL,
    additional_rent numeric(19,2) NOT NULL,
    additional_electricity numeric(19,2) NOT NULL,
    additional_water numeric(19,2) NOT NULL,
    damage_charge numeric(15,2) NOT NULL,
    damage_reason character varying(2000),
    total_due numeric(19,2) NOT NULL,
    deposit_amount numeric(19,2) NOT NULL,
    deposit_applied numeric(19,2) NOT NULL,
    amount_to_collect numeric(19,2) NOT NULL,
    amount_to_refund numeric(19,2) NOT NULL,
    collected_amount numeric(19,2) NOT NULL,
    refunded_amount numeric(19,2) NOT NULL,
    payment_method character varying(30) NOT NULL,
    settled_by uuid NOT NULL,
    created_at timestamp with time zone NOT NULL,
    invoice_balances jsonb NOT NULL,
    CONSTRAINT ck_damage_reason CHECK (((damage_charge = (0)::numeric) OR ((damage_reason IS NOT NULL) AND (length(TRIM(BOTH FROM damage_reason)) > 0)))),
    CONSTRAINT ck_settlement_charges CHECK (((monthly_rent > (0)::numeric) AND (electricity_unit_price >= (0)::numeric) AND (water_unit_price >= (0)::numeric) AND (invoice_debt >= (0)::numeric) AND (damage_charge >= (0)::numeric) AND (additional_rent = (monthly_rent * (unbilled_months)::numeric)) AND (additional_electricity = round(((final_electricity - previous_electricity) * electricity_unit_price), 2)) AND (additional_water = round(((final_water - previous_water) * water_unit_price), 2)) AND (total_due = ((((invoice_debt + additional_rent) + additional_electricity) + additional_water) + damage_charge)))),
    CONSTRAINT ck_settlement_funds CHECK (((deposit_amount >= (0)::numeric) AND (deposit_applied = LEAST(deposit_amount, total_due)) AND (amount_to_collect = (total_due - deposit_applied)) AND (amount_to_refund = (deposit_amount - deposit_applied)) AND (collected_amount = amount_to_collect) AND (refunded_amount = amount_to_refund))),
    CONSTRAINT ck_settlement_readings CHECK (((previous_electricity >= (0)::numeric) AND (final_electricity >= previous_electricity) AND (previous_water >= (0)::numeric) AND (final_water >= previous_water))),
    CONSTRAINT tenancy_settlements_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['CASH'::character varying, 'BANK_TRANSFER'::character varying])::text[]))),
    CONSTRAINT tenancy_settlements_unbilled_months_check CHECK ((unbilled_months >= 0))
);


--
-- Name: vnpay_payment_attempts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vnpay_payment_attempts (
    id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    txn_ref character varying(100) NOT NULL,
    amount numeric(19,2) NOT NULL,
    status character varying(20) NOT NULL,
    payment_url text NOT NULL,
    response_code character varying(10),
    transaction_no character varying(50),
    created_at timestamp with time zone NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    completed_at timestamp with time zone,
    CONSTRAINT vnpay_payment_attempts_amount_check CHECK ((amount > (0)::numeric)),
    CONSTRAINT vnpay_payment_attempts_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying, 'EXPIRED'::character varying])::text[])))
);


--
-- Data for Name: active_tenancies; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.active_tenancies (id, room_id, contract_id, tenant_id, check_in_date, assigned_at) FROM stdin;
\.


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.audit_logs (id, user_id, action, entity_name, entity_id, old_values, new_values, created_at) FROM stdin;
\.


--
-- Data for Name: contact_cards; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.contact_cards (id, rental_request_id, landlord_id, landlord_name, landlord_phone, landlord_zalo, landlord_avatar_url, note, issued_at) FROM stdin;
\.


--
-- Data for Name: contracts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.contracts (id, contract_code, rental_request_id, room_id, landlord_id, tenant_id, start_date, end_date, actual_return_date, monthly_rent, deposit_amount, electricity_unit_price, water_unit_price, initial_electricity, initial_water, status, terms_and_conditions, cancellation_reason, deposit_confirmed_at, created_at, updated_at, deposit_deadline) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create rental business	SQL	V1__create_rental_business.sql	-960565096	postgres	2026-10-17 17:27:41.390523	118	t
2	2	add reservation deadlines	SQL	V2__add_reservation_deadlines.sql	946961439	postgres	2026-10-17 17:27:41.583288	11	t
3	3	create invoice reminders	SQL	V3__create_invoice_reminders.sql	-210197608	postgres	2026-10-17 17:27:41.618145	27	t
4	4	add invoice corrections and settlements	SQL	V4__add_invoice_corrections_and_settlements.sql	1624022467	postgres	2026-10-17 17:27:41.663247	37	t
5	5	add outbox retries	SQL	V5__add_outbox_retries.sql	1916129761	postgres	2026-10-17 17:27:41.720592	10	t
6	6	create vnpay payment attempts	SQL	V6__create_vnpay_payment_attempts.sql	282435028	postgres	2026-10-25 15:32:50.535332	166	t
7	7	create contact cards	SQL	V7__create_contact_cards.sql	-1342761780	postgres	2026-10-27 12:37:37.770588	287	t
8	8	create audit logs	SQL	V8__create_audit_logs.sql	-208831268	postgres	2026-10-31 06:53:07.587812	98	t
\.


--
-- Data for Name: invoice_corrections; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.invoice_corrections (id, invoice_id, revision, corrected_by, reason, old_values, new_values, created_at) FROM stdin;
\.


--
-- Data for Name: invoice_items; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.invoice_items (id, invoice_id, item_name, item_type, quantity, unit_price, total_price) FROM stdin;
\.


--
-- Data for Name: invoice_reminders; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.invoice_reminders (id, invoice_id, tenant_id, landlord_id, reminder_date, outstanding_amount, content, created_at) FROM stdin;
\.


--
-- Data for Name: invoices; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.invoices (id, invoice_code, contract_id, meter_reading_id, billing_month, due_date, total_amount, paid_amount, status, is_disputed, dispute_reason, dispute_resolution, confirmed_at, payment_date, payment_method, created_at, updated_at, revision) FROM stdin;
\.


--
-- Data for Name: meter_readings; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.meter_readings (id, contract_id, room_id, billing_month, old_electricity_val, new_electricity_val, electricity_usage, old_water_val, new_water_val, water_usage, recorded_by, reading_date, created_at) FROM stdin;
\.


--
-- Data for Name: rental_requests; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.rental_requests (id, room_id, post_id, tenant_id, landlord_id, note, status, reject_reason, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: room_booking_locks; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.room_booking_locks (room_id) FROM stdin;
\.


--
-- Data for Name: tenancy_events; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tenancy_events (sequence, event_id, event_type, room_id, tenancy_id, partition_key, topic, occurred_at, published_at, attempt_count, next_attempt_at, last_error) FROM stdin;
\.


--
-- Data for Name: tenancy_settlements; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tenancy_settlements (id, contract_id, actual_return_date, last_billed_month, unbilled_months, previous_electricity, final_electricity, previous_water, final_water, monthly_rent, electricity_unit_price, water_unit_price, invoice_debt, additional_rent, additional_electricity, additional_water, damage_charge, damage_reason, total_due, deposit_amount, deposit_applied, amount_to_collect, amount_to_refund, collected_amount, refunded_amount, payment_method, settled_by, created_at, invoice_balances) FROM stdin;
\.


--
-- Data for Name: vnpay_payment_attempts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.vnpay_payment_attempts (id, invoice_id, txn_ref, amount, status, payment_url, response_code, transaction_no, created_at, expires_at, completed_at) FROM stdin;
\.


--
-- Name: tenancy_events_sequence_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tenancy_events_sequence_seq', 12, true);


--
-- Name: active_tenancies active_tenancies_contract_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.active_tenancies
    ADD CONSTRAINT active_tenancies_contract_id_key UNIQUE (contract_id);


--
-- Name: active_tenancies active_tenancies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.active_tenancies
    ADD CONSTRAINT active_tenancies_pkey PRIMARY KEY (id);


--
-- Name: active_tenancies active_tenancies_room_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.active_tenancies
    ADD CONSTRAINT active_tenancies_room_id_key UNIQUE (room_id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: contact_cards contact_cards_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contact_cards
    ADD CONSTRAINT contact_cards_pkey PRIMARY KEY (id);


--
-- Name: contact_cards contact_cards_rental_request_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contact_cards
    ADD CONSTRAINT contact_cards_rental_request_id_key UNIQUE (rental_request_id);


--
-- Name: contracts contracts_contract_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contracts
    ADD CONSTRAINT contracts_contract_code_key UNIQUE (contract_code);


--
-- Name: contracts contracts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contracts
    ADD CONSTRAINT contracts_pkey PRIMARY KEY (id);


--
-- Name: contracts contracts_rental_request_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contracts
    ADD CONSTRAINT contracts_rental_request_id_key UNIQUE (rental_request_id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: invoice_corrections invoice_corrections_invoice_id_revision_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_corrections
    ADD CONSTRAINT invoice_corrections_invoice_id_revision_key UNIQUE (invoice_id, revision);


--
-- Name: invoice_corrections invoice_corrections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_corrections
    ADD CONSTRAINT invoice_corrections_pkey PRIMARY KEY (id);


--
-- Name: invoice_items invoice_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_items
    ADD CONSTRAINT invoice_items_pkey PRIMARY KEY (id);


--
-- Name: invoice_reminders invoice_reminders_invoice_id_reminder_date_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_reminders
    ADD CONSTRAINT invoice_reminders_invoice_id_reminder_date_key UNIQUE (invoice_id, reminder_date);


--
-- Name: invoice_reminders invoice_reminders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_reminders
    ADD CONSTRAINT invoice_reminders_pkey PRIMARY KEY (id);


--
-- Name: invoices invoices_invoice_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_invoice_code_key UNIQUE (invoice_code);


--
-- Name: invoices invoices_meter_reading_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_meter_reading_id_key UNIQUE (meter_reading_id);


--
-- Name: invoices invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (id);


--
-- Name: meter_readings meter_readings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT meter_readings_pkey PRIMARY KEY (id);


--
-- Name: rental_requests rental_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rental_requests
    ADD CONSTRAINT rental_requests_pkey PRIMARY KEY (id);


--
-- Name: room_booking_locks room_booking_locks_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.room_booking_locks
    ADD CONSTRAINT room_booking_locks_pkey PRIMARY KEY (room_id);


--
-- Name: tenancy_events tenancy_events_event_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenancy_events
    ADD CONSTRAINT tenancy_events_event_id_key UNIQUE (event_id);


--
-- Name: tenancy_events tenancy_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenancy_events
    ADD CONSTRAINT tenancy_events_pkey PRIMARY KEY (sequence);


--
-- Name: tenancy_settlements tenancy_settlements_contract_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenancy_settlements
    ADD CONSTRAINT tenancy_settlements_contract_id_key UNIQUE (contract_id);


--
-- Name: tenancy_settlements tenancy_settlements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenancy_settlements
    ADD CONSTRAINT tenancy_settlements_pkey PRIMARY KEY (id);


--
-- Name: invoices uq_contract_invoice_month; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT uq_contract_invoice_month UNIQUE (contract_id, billing_month);


--
-- Name: meter_readings uq_contract_reading_month; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT uq_contract_reading_month UNIQUE (contract_id, billing_month);


--
-- Name: invoice_items uq_invoice_item_type; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_items
    ADD CONSTRAINT uq_invoice_item_type UNIQUE (invoice_id, item_type);


--
-- Name: vnpay_payment_attempts vnpay_payment_attempts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vnpay_payment_attempts
    ADD CONSTRAINT vnpay_payment_attempts_pkey PRIMARY KEY (id);


--
-- Name: vnpay_payment_attempts vnpay_payment_attempts_txn_ref_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vnpay_payment_attempts
    ADD CONSTRAINT vnpay_payment_attempts_txn_ref_key UNIQUE (txn_ref);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_contact_cards_landlord; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contact_cards_landlord ON public.contact_cards USING btree (landlord_id, issued_at DESC);


--
-- Name: ix_audit_logs_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_audit_logs_action ON public.audit_logs USING btree (action);


--
-- Name: ix_audit_logs_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_audit_logs_created_at ON public.audit_logs USING btree (created_at DESC);


--
-- Name: ix_audit_logs_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_audit_logs_entity ON public.audit_logs USING btree (entity_name, entity_id);


--
-- Name: ix_contract_landlord; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_contract_landlord ON public.contracts USING btree (landlord_id, created_at DESC);


--
-- Name: ix_contract_tenant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_contract_tenant ON public.contracts USING btree (tenant_id, created_at DESC);


--
-- Name: ix_event_room_unpublished; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_event_room_unpublished ON public.tenancy_events USING btree (room_id, sequence) WHERE (published_at IS NULL);


--
-- Name: ix_event_unpublished; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_event_unpublished ON public.tenancy_events USING btree (sequence) WHERE (published_at IS NULL);


--
-- Name: ix_pending_deposit_deadline; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_pending_deposit_deadline ON public.contracts USING btree (deposit_deadline) WHERE ((status)::text = 'PENDING'::text);


--
-- Name: ix_reminder_tenant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_reminder_tenant ON public.invoice_reminders USING btree (tenant_id, created_at DESC);


--
-- Name: ix_request_landlord; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_request_landlord ON public.rental_requests USING btree (landlord_id, created_at DESC);


--
-- Name: ix_request_tenant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_request_tenant ON public.rental_requests USING btree (tenant_id, created_at DESC);


--
-- Name: ix_unpaid_invoice_due_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_unpaid_invoice_due_date ON public.invoices USING btree (due_date) WHERE (paid_amount < total_amount);


--
-- Name: ix_vnpay_invoice; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_vnpay_invoice ON public.vnpay_payment_attempts USING btree (invoice_id, created_at DESC);


--
-- Name: uq_pending_request; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_pending_request ON public.rental_requests USING btree (room_id, tenant_id) WHERE ((status)::text = 'PENDING'::text);


--
-- Name: uq_room_open_contract; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_room_open_contract ON public.contracts USING btree (room_id) WHERE ((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACTIVE'::character varying])::text[]));


--
-- Name: uq_vnpay_pending_invoice; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_vnpay_pending_invoice ON public.vnpay_payment_attempts USING btree (invoice_id) WHERE ((status)::text = 'PENDING'::text);


--
-- Name: active_tenancies fk_active_contract; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.active_tenancies
    ADD CONSTRAINT fk_active_contract FOREIGN KEY (contract_id) REFERENCES public.contracts(id);


--
-- Name: contact_cards fk_contact_card_request; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contact_cards
    ADD CONSTRAINT fk_contact_card_request FOREIGN KEY (rental_request_id) REFERENCES public.rental_requests(id) ON DELETE CASCADE;


--
-- Name: contracts fk_contract_request; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contracts
    ADD CONSTRAINT fk_contract_request FOREIGN KEY (rental_request_id) REFERENCES public.rental_requests(id);


--
-- Name: invoices fk_invoice_contract; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fk_invoice_contract FOREIGN KEY (contract_id) REFERENCES public.contracts(id);


--
-- Name: invoices fk_invoice_reading; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fk_invoice_reading FOREIGN KEY (meter_reading_id) REFERENCES public.meter_readings(id);


--
-- Name: invoice_items fk_item_invoice; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_items
    ADD CONSTRAINT fk_item_invoice FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: meter_readings fk_reading_contract; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT fk_reading_contract FOREIGN KEY (contract_id) REFERENCES public.contracts(id);


--
-- Name: invoice_corrections invoice_corrections_invoice_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_corrections
    ADD CONSTRAINT invoice_corrections_invoice_id_fkey FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: invoice_reminders invoice_reminders_invoice_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_reminders
    ADD CONSTRAINT invoice_reminders_invoice_id_fkey FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: tenancy_events tenancy_events_tenancy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenancy_events
    ADD CONSTRAINT tenancy_events_tenancy_id_fkey FOREIGN KEY (tenancy_id) REFERENCES public.contracts(id);


--
-- Name: tenancy_settlements tenancy_settlements_contract_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tenancy_settlements
    ADD CONSTRAINT tenancy_settlements_contract_id_fkey FOREIGN KEY (contract_id) REFERENCES public.contracts(id);


--
-- Name: vnpay_payment_attempts vnpay_payment_attempts_invoice_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vnpay_payment_attempts
    ADD CONSTRAINT vnpay_payment_attempts_invoice_id_fkey FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- PostgreSQL database dump complete
--

\unrestrict EdC0HiR8VNa1T7K9hB1NcuOnTAwxim986I532DzdRvStxEgpmLy6XewI0GSRxlR

