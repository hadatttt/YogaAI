--
-- PostgreSQL database dump
--

\restrict 21czpZD7fq3TAWL3Ew2L6lhTkv9o37339ZgfYczO7Gqy34odAtbB5tS4mtBe33a

-- Dumped from database version 17.6
-- Dumped by pg_dump version 17.6

-- Started on 2025-10-02 03:08:17

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
-- TOC entry 217 (class 1259 OID 16389)
-- Name: brands; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.brands (
    brand_id uuid NOT NULL,
    brand_name character varying(100) NOT NULL,
    created_at timestamp(6) without time zone,
    description character varying(255),
    is_active boolean
);


ALTER TABLE public.brands OWNER TO postgres;

--
-- TOC entry 218 (class 1259 OID 16394)
-- Name: cart_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.cart_items (
    cart_item_id uuid NOT NULL,
    added_at timestamp(6) without time zone,
    quantity integer NOT NULL,
    cart_id uuid NOT NULL,
    variant_id uuid NOT NULL
);


ALTER TABLE public.cart_items OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 16399)
-- Name: categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.categories (
    category_id uuid NOT NULL,
    category_name character varying(100) NOT NULL,
    created_at timestamp(6) without time zone,
    description character varying(255),
    image_url character varying(500),
    is_active boolean,
    sort_order integer,
    parent_category_id uuid
);


ALTER TABLE public.categories OWNER TO postgres;

--
-- TOC entry 220 (class 1259 OID 16406)
-- Name: coupons; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.coupons (
    coupon_id uuid NOT NULL,
    code character varying(50) NOT NULL,
    created_at timestamp(6) without time zone,
    discount_type character varying(255) NOT NULL,
    discount_value double precision NOT NULL,
    end_date timestamp(6) without time zone NOT NULL,
    start_date timestamp(6) without time zone NOT NULL,
    usage_limit integer,
    CONSTRAINT coupons_discount_type_check CHECK (((discount_type)::text = ANY ((ARRAY['PERCENTAGE'::character varying, 'FIXED_AMOUNT'::character varying])::text[])))
);


ALTER TABLE public.coupons OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 16412)
-- Name: invalidated_token; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.invalidated_token (
    id character varying(255) NOT NULL,
    expiry_time timestamp(6) without time zone
);


ALTER TABLE public.invalidated_token OWNER TO postgres;

--
-- TOC entry 222 (class 1259 OID 16417)
-- Name: inventories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.inventories (
    inventory_id character varying(255) NOT NULL,
    quantity_available integer NOT NULL,
    quantity_reserved integer NOT NULL,
    updated_at timestamp(6) without time zone,
    variant_id uuid NOT NULL
);


ALTER TABLE public.inventories OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 16422)
-- Name: notifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notifications (
    notification_id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    is_read boolean,
    message text,
    title character varying(200) NOT NULL,
    profile_id uuid NOT NULL
);


ALTER TABLE public.notifications OWNER TO postgres;

--
-- TOC entry 224 (class 1259 OID 16429)
-- Name: order_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_items (
    order_item_id uuid NOT NULL,
    quantity integer NOT NULL,
    total_price numeric(12,2) NOT NULL,
    unit_price numeric(12,2) NOT NULL,
    order_id uuid NOT NULL,
    variant_id uuid
);


ALTER TABLE public.order_items OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 16434)
-- Name: orders; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.orders (
    order_id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    order_status character varying(255),
    total_amount numeric(12,2) NOT NULL,
    updated_at timestamp(6) without time zone,
    user_id uuid,
    CONSTRAINT orders_order_status_check CHECK (((order_status)::text = ANY ((ARRAY['pending'::character varying, 'confirmed'::character varying, 'processing'::character varying, 'shipped'::character varying, 'delivered'::character varying, 'cancelled'::character varying])::text[])))
);


ALTER TABLE public.orders OWNER TO postgres;

--
-- TOC entry 226 (class 1259 OID 16440)
-- Name: payments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payments (
    payment_id uuid NOT NULL,
    amount numeric(12,2) NOT NULL,
    created_at timestamp(6) without time zone,
    payment_method character varying(50) NOT NULL,
    payment_status character varying(255),
    updated_at timestamp(6) without time zone,
    order_id uuid,
    CONSTRAINT payments_payment_status_check CHECK (((payment_status)::text = ANY ((ARRAY['PENDING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[])))
);


ALTER TABLE public.payments OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 16446)
-- Name: permission; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.permission (
    permission_id uuid NOT NULL,
    description character varying(255),
    name character varying(255) NOT NULL
);


ALTER TABLE public.permission OWNER TO postgres;

--
-- TOC entry 229 (class 1259 OID 16454)
-- Name: product_images; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_images (
    image_id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    image_url character varying(255) NOT NULL,
    is_main boolean,
    updated_at timestamp(6) without time zone,
    product_id uuid NOT NULL
);


ALTER TABLE public.product_images OWNER TO postgres;

--
-- TOC entry 228 (class 1259 OID 16453)
-- Name: product_images_image_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.product_images_image_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.product_images_image_id_seq OWNER TO postgres;

--
-- TOC entry 5050 (class 0 OID 0)
-- Dependencies: 228
-- Name: product_images_image_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.product_images_image_id_seq OWNED BY public.product_images.image_id;


--
-- TOC entry 230 (class 1259 OID 16460)
-- Name: product_variant; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_variant (
    variant_id uuid NOT NULL,
    color character varying(255) NOT NULL,
    created_at timestamp(6) without time zone,
    is_active boolean,
    price numeric(10,2) NOT NULL,
    size character varying(255) NOT NULL,
    stock_quantity integer NOT NULL,
    updated_at timestamp(6) without time zone,
    product_id uuid NOT NULL
);


ALTER TABLE public.product_variant OWNER TO postgres;

--
-- TOC entry 231 (class 1259 OID 16467)
-- Name: products; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.products (
    product_id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    description text,
    gender character varying(255),
    is_active boolean,
    product_name character varying(200) NOT NULL,
    season character varying(255),
    updated_at timestamp(6) without time zone,
    brand_id uuid NOT NULL,
    category_id uuid NOT NULL,
    CONSTRAINT products_gender_check CHECK (((gender)::text = ANY ((ARRAY['men'::character varying, 'women'::character varying, 'unisex'::character varying, 'kids'::character varying])::text[]))),
    CONSTRAINT products_season_check CHECK (((season)::text = ANY ((ARRAY['spring'::character varying, 'summer'::character varying, 'autumn'::character varying, 'winter'::character varying, 'all_season'::character varying])::text[])))
);


ALTER TABLE public.products OWNER TO postgres;

--
-- TOC entry 232 (class 1259 OID 16476)
-- Name: reviews; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.reviews (
    review_id uuid NOT NULL,
    comment character varying(1000),
    created_at timestamp(6) without time zone,
    rating integer NOT NULL,
    updated_at timestamp(6) without time zone,
    product_id uuid NOT NULL,
    profile_id uuid NOT NULL
);


ALTER TABLE public.reviews OWNER TO postgres;

--
-- TOC entry 233 (class 1259 OID 16483)
-- Name: role_permissions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.role_permissions (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL
);


ALTER TABLE public.role_permissions OWNER TO postgres;

--
-- TOC entry 234 (class 1259 OID 16488)
-- Name: roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.roles (
    role_id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    description character varying(255),
    role_name character varying(50) NOT NULL
);


ALTER TABLE public.roles OWNER TO postgres;

--
-- TOC entry 235 (class 1259 OID 16493)
-- Name: shipping_methods; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.shipping_methods (
    shipping_id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    estimated_days integer,
    method_name character varying(100) NOT NULL,
    price double precision NOT NULL
);


ALTER TABLE public.shipping_methods OWNER TO postgres;

--
-- TOC entry 236 (class 1259 OID 16498)
-- Name: shopping_carts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.shopping_carts (
    cart_id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    profile_id uuid
);


ALTER TABLE public.shopping_carts OWNER TO postgres;

--
-- TOC entry 237 (class 1259 OID 16503)
-- Name: support_messages; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.support_messages (
    message_id uuid NOT NULL,
    content text NOT NULL,
    created_at timestamp(6) without time zone,
    profile_id uuid NOT NULL,
    ticket_id uuid NOT NULL
);


ALTER TABLE public.support_messages OWNER TO postgres;

--
-- TOC entry 238 (class 1259 OID 16510)
-- Name: support_tickets; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.support_tickets (
    ticket_id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    message text NOT NULL,
    status character varying(255) NOT NULL,
    subject character varying(200) NOT NULL,
    updated_at timestamp(6) without time zone,
    profile_id uuid NOT NULL,
    CONSTRAINT support_tickets_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'IN_PROGRESS'::character varying, 'CLOSED'::character varying])::text[])))
);


ALTER TABLE public.support_tickets OWNER TO postgres;

--
-- TOC entry 239 (class 1259 OID 16518)
-- Name: transactions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.transactions (
    transaction_id uuid NOT NULL,
    amount double precision NOT NULL,
    payment_method character varying(50) NOT NULL,
    status character varying(255) NOT NULL,
    transaction_time timestamp(6) without time zone,
    order_id uuid NOT NULL,
    profile_id uuid NOT NULL,
    CONSTRAINT transactions_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying])::text[])))
);


ALTER TABLE public.transactions OWNER TO postgres;

--
-- TOC entry 240 (class 1259 OID 16524)
-- Name: user_account; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_account (
    account_id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    email character varying(100) NOT NULL,
    is_active boolean,
    is_verified boolean,
    last_login_at timestamp(6) without time zone,
    password_hash character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    username character varying(50) NOT NULL,
    verification_code character varying(6)
);


ALTER TABLE public.user_account OWNER TO postgres;

--
-- TOC entry 241 (class 1259 OID 16529)
-- Name: user_addresses; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_addresses (
    address_id uuid NOT NULL,
    address_line1 character varying(200) NOT NULL,
    address_line2 character varying(200),
    address_type character varying(255),
    city character varying(100) NOT NULL,
    country character varying(100) NOT NULL,
    created_at timestamp(6) without time zone,
    is_default boolean,
    phone character varying(20),
    postal_code character varying(20),
    recipient_name character varying(100) NOT NULL,
    state_province character varying(100),
    profile_id uuid NOT NULL,
    CONSTRAINT user_addresses_address_type_check CHECK (((address_type)::text = ANY ((ARRAY['shipping'::character varying, 'billing'::character varying])::text[])))
);


ALTER TABLE public.user_addresses OWNER TO postgres;

--
-- TOC entry 242 (class 1259 OID 16537)
-- Name: user_preferences; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_preferences (
    preference_id uuid NOT NULL,
    budget_max numeric(12,2),
    budget_min numeric(12,2),
    created_at timestamp(6) without time zone,
    notification_settings jsonb,
    preferred_colors text[],
    preferred_size character varying(10),
    style_preferences text[],
    updated_at timestamp(6) without time zone,
    profile_id uuid NOT NULL
);


ALTER TABLE public.user_preferences OWNER TO postgres;

--
-- TOC entry 243 (class 1259 OID 16544)
-- Name: user_profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_profiles (
    profile_id uuid NOT NULL,
    avatar_url character varying(500),
    date_of_birth date,
    full_name character varying(255),
    gender character varying(255),
    phone character varying(255),
    account_id uuid NOT NULL,
    CONSTRAINT user_profiles_gender_check CHECK (((gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying, 'OTHER'::character varying])::text[])))
);


ALTER TABLE public.user_profiles OWNER TO postgres;

--
-- TOC entry 244 (class 1259 OID 16552)
-- Name: user_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_roles (
    account_id uuid NOT NULL,
    role_id uuid NOT NULL
);


ALTER TABLE public.user_roles OWNER TO postgres;

--
-- TOC entry 245 (class 1259 OID 16557)
-- Name: wishlist_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.wishlist_items (
    wishlist_item_id uuid NOT NULL,
    added_at timestamp(6) without time zone,
    product_id uuid NOT NULL,
    wishlist_id uuid NOT NULL
);


ALTER TABLE public.wishlist_items OWNER TO postgres;

--
-- TOC entry 246 (class 1259 OID 16562)
-- Name: wishlists; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.wishlists (
    wishlist_id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    name character varying(100),
    profile_id uuid NOT NULL
);


ALTER TABLE public.wishlists OWNER TO postgres;

--
-- TOC entry 4753 (class 2604 OID 16457)
-- Name: product_images image_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_images ALTER COLUMN image_id SET DEFAULT nextval('public.product_images_image_id_seq'::regclass);


--
-- TOC entry 5015 (class 0 OID 16389)
-- Dependencies: 217
-- Data for Name: brands; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.brands (brand_id, brand_name, created_at, description, is_active) FROM stdin;
\.


--
-- TOC entry 5016 (class 0 OID 16394)
-- Dependencies: 218
-- Data for Name: cart_items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.cart_items (cart_item_id, added_at, quantity, cart_id, variant_id) FROM stdin;
\.


--
-- TOC entry 5017 (class 0 OID 16399)
-- Dependencies: 219
-- Data for Name: categories; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.categories (category_id, category_name, created_at, description, image_url, is_active, sort_order, parent_category_id) FROM stdin;
\.


--
-- TOC entry 5018 (class 0 OID 16406)
-- Dependencies: 220
-- Data for Name: coupons; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.coupons (coupon_id, code, created_at, discount_type, discount_value, end_date, start_date, usage_limit) FROM stdin;
\.


--
-- TOC entry 5019 (class 0 OID 16412)
-- Dependencies: 221
-- Data for Name: invalidated_token; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.invalidated_token (id, expiry_time) FROM stdin;
\.


--
-- TOC entry 5020 (class 0 OID 16417)
-- Dependencies: 222
-- Data for Name: inventories; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.inventories (inventory_id, quantity_available, quantity_reserved, updated_at, variant_id) FROM stdin;
\.


--
-- TOC entry 5021 (class 0 OID 16422)
-- Dependencies: 223
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notifications (notification_id, created_at, is_read, message, title, profile_id) FROM stdin;
\.


--
-- TOC entry 5022 (class 0 OID 16429)
-- Dependencies: 224
-- Data for Name: order_items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.order_items (order_item_id, quantity, total_price, unit_price, order_id, variant_id) FROM stdin;
\.


--
-- TOC entry 5023 (class 0 OID 16434)
-- Dependencies: 225
-- Data for Name: orders; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.orders (order_id, created_at, order_status, total_amount, updated_at, user_id) FROM stdin;
\.


--
-- TOC entry 5024 (class 0 OID 16440)
-- Dependencies: 226
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.payments (payment_id, amount, created_at, payment_method, payment_status, updated_at, order_id) FROM stdin;
\.


--
-- TOC entry 5025 (class 0 OID 16446)
-- Dependencies: 227
-- Data for Name: permission; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.permission (permission_id, description, name) FROM stdin;
eb5c3bff-aa6e-4703-86d1-85fc987eefd6	PRODUCT_MODULE - PRODUCT_CREATE	PRODUCT_CREATE
e8d418ef-30ca-4f2d-b268-0fd1d58f3a7f	PRODUCT_MODULE - PRODUCT_READ	PRODUCT_READ
1e24041c-b917-4309-88db-19778c4d5f5c	USER_MODULE - USER_READ	USER_READ
ac273542-cf4c-41c9-a822-a8507c2f6bba	USER_MODULE - USER_WRITE	USER_WRITE
\.


--
-- TOC entry 5027 (class 0 OID 16454)
-- Dependencies: 229
-- Data for Name: product_images; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.product_images (image_id, created_at, image_url, is_main, updated_at, product_id) FROM stdin;
\.


--
-- TOC entry 5028 (class 0 OID 16460)
-- Dependencies: 230
-- Data for Name: product_variant; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.product_variant (variant_id, color, created_at, is_active, price, size, stock_quantity, updated_at, product_id) FROM stdin;
\.


--
-- TOC entry 5029 (class 0 OID 16467)
-- Dependencies: 231
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.products (product_id, created_at, description, gender, is_active, product_name, season, updated_at, brand_id, category_id) FROM stdin;
\.


--
-- TOC entry 5030 (class 0 OID 16476)
-- Dependencies: 232
-- Data for Name: reviews; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.reviews (review_id, comment, created_at, rating, updated_at, product_id, profile_id) FROM stdin;
\.


--
-- TOC entry 5031 (class 0 OID 16483)
-- Dependencies: 233
-- Data for Name: role_permissions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.role_permissions (role_id, permission_id) FROM stdin;
a0c2838f-e05e-4c4e-9c3d-d86502dbafcc	ac273542-cf4c-41c9-a822-a8507c2f6bba
a0c2838f-e05e-4c4e-9c3d-d86502dbafcc	1e24041c-b917-4309-88db-19778c4d5f5c
16705367-fdc2-40be-9b5b-a635f8a70e31	e8d418ef-30ca-4f2d-b268-0fd1d58f3a7f
16705367-fdc2-40be-9b5b-a635f8a70e31	ac273542-cf4c-41c9-a822-a8507c2f6bba
16705367-fdc2-40be-9b5b-a635f8a70e31	eb5c3bff-aa6e-4703-86d1-85fc987eefd6
16705367-fdc2-40be-9b5b-a635f8a70e31	1e24041c-b917-4309-88db-19778c4d5f5c
\.


--
-- TOC entry 5032 (class 0 OID 16488)
-- Dependencies: 234
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.roles (role_id, created_at, description, role_name) FROM stdin;
a0c2838f-e05e-4c4e-9c3d-d86502dbafcc	2025-09-19 15:22:49.112825	User role	USER
16705367-fdc2-40be-9b5b-a635f8a70e31	2025-09-19 15:22:49.207918	Admin role	ADMIN
\.


--
-- TOC entry 5033 (class 0 OID 16493)
-- Dependencies: 235
-- Data for Name: shipping_methods; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.shipping_methods (shipping_id, created_at, estimated_days, method_name, price) FROM stdin;
\.


--
-- TOC entry 5034 (class 0 OID 16498)
-- Dependencies: 236
-- Data for Name: shopping_carts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.shopping_carts (cart_id, created_at, updated_at, profile_id) FROM stdin;
\.


--
-- TOC entry 5035 (class 0 OID 16503)
-- Dependencies: 237
-- Data for Name: support_messages; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.support_messages (message_id, content, created_at, profile_id, ticket_id) FROM stdin;
\.


--
-- TOC entry 5036 (class 0 OID 16510)
-- Dependencies: 238
-- Data for Name: support_tickets; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.support_tickets (ticket_id, created_at, message, status, subject, updated_at, profile_id) FROM stdin;
\.


--
-- TOC entry 5037 (class 0 OID 16518)
-- Dependencies: 239
-- Data for Name: transactions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.transactions (transaction_id, amount, payment_method, status, transaction_time, order_id, profile_id) FROM stdin;
\.


--
-- TOC entry 5038 (class 0 OID 16524)
-- Dependencies: 240
-- Data for Name: user_account; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_account (account_id, created_at, email, is_active, is_verified, last_login_at, password_hash, updated_at, username, verification_code) FROM stdin;
9cbe7f08-53cf-4be0-9f28-c09c8d42693a	2025-09-19 15:22:52.063173	admin123@gmail.com	t	t	\N	$2a$15$uQ3K/.x.f5defKqxmEDZEuqZAVozBdwacEB7xFJnRyHtE6GY69jWK	2025-09-19 15:22:52.063173	admin	\N
\.


--
-- TOC entry 5039 (class 0 OID 16529)
-- Dependencies: 241
-- Data for Name: user_addresses; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_addresses (address_id, address_line1, address_line2, address_type, city, country, created_at, is_default, phone, postal_code, recipient_name, state_province, profile_id) FROM stdin;
\.


--
-- TOC entry 5040 (class 0 OID 16537)
-- Dependencies: 242
-- Data for Name: user_preferences; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_preferences (preference_id, budget_max, budget_min, created_at, notification_settings, preferred_colors, preferred_size, style_preferences, updated_at, profile_id) FROM stdin;
\.


--
-- TOC entry 5041 (class 0 OID 16544)
-- Dependencies: 243
-- Data for Name: user_profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_profiles (profile_id, avatar_url, date_of_birth, full_name, gender, phone, account_id) FROM stdin;
a9972bf4-d0c8-48bf-8e86-51b5c4e71098	\N	\N	System Administrator	\N	\N	9cbe7f08-53cf-4be0-9f28-c09c8d42693a
\.


--
-- TOC entry 5042 (class 0 OID 16552)
-- Dependencies: 244
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_roles (account_id, role_id) FROM stdin;
9cbe7f08-53cf-4be0-9f28-c09c8d42693a	16705367-fdc2-40be-9b5b-a635f8a70e31
\.


--
-- TOC entry 5043 (class 0 OID 16557)
-- Dependencies: 245
-- Data for Name: wishlist_items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.wishlist_items (wishlist_item_id, added_at, product_id, wishlist_id) FROM stdin;
\.


--
-- TOC entry 5044 (class 0 OID 16562)
-- Dependencies: 246
-- Data for Name: wishlists; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.wishlists (wishlist_id, created_at, name, profile_id) FROM stdin;
\.


--
-- TOC entry 5051 (class 0 OID 0)
-- Dependencies: 228
-- Name: product_images_image_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.product_images_image_id_seq', 1, false);


--
-- TOC entry 4764 (class 2606 OID 16393)
-- Name: brands brands_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.brands
    ADD CONSTRAINT brands_pkey PRIMARY KEY (brand_id);


--
-- TOC entry 4768 (class 2606 OID 16398)
-- Name: cart_items cart_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (cart_item_id);


--
-- TOC entry 4770 (class 2606 OID 16405)
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (category_id);


--
-- TOC entry 4772 (class 2606 OID 16411)
-- Name: coupons coupons_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.coupons
    ADD CONSTRAINT coupons_pkey PRIMARY KEY (coupon_id);


--
-- TOC entry 4776 (class 2606 OID 16416)
-- Name: invalidated_token invalidated_token_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.invalidated_token
    ADD CONSTRAINT invalidated_token_pkey PRIMARY KEY (id);


--
-- TOC entry 4778 (class 2606 OID 16421)
-- Name: inventories inventories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inventories
    ADD CONSTRAINT inventories_pkey PRIMARY KEY (inventory_id);


--
-- TOC entry 4782 (class 2606 OID 16428)
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (notification_id);


--
-- TOC entry 4784 (class 2606 OID 16433)
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (order_item_id);


--
-- TOC entry 4786 (class 2606 OID 16439)
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (order_id);


--
-- TOC entry 4788 (class 2606 OID 16445)
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (payment_id);


--
-- TOC entry 4790 (class 2606 OID 16452)
-- Name: permission permission_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permission
    ADD CONSTRAINT permission_pkey PRIMARY KEY (permission_id);


--
-- TOC entry 4794 (class 2606 OID 16459)
-- Name: product_images product_images_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_images
    ADD CONSTRAINT product_images_pkey PRIMARY KEY (image_id);


--
-- TOC entry 4796 (class 2606 OID 16466)
-- Name: product_variant product_variant_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_variant
    ADD CONSTRAINT product_variant_pkey PRIMARY KEY (variant_id);


--
-- TOC entry 4798 (class 2606 OID 16475)
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (product_id);


--
-- TOC entry 4800 (class 2606 OID 16482)
-- Name: reviews reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (review_id);


--
-- TOC entry 4802 (class 2606 OID 16487)
-- Name: role_permissions role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id);


--
-- TOC entry 4804 (class 2606 OID 16492)
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (role_id);


--
-- TOC entry 4808 (class 2606 OID 16497)
-- Name: shipping_methods shipping_methods_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.shipping_methods
    ADD CONSTRAINT shipping_methods_pkey PRIMARY KEY (shipping_id);


--
-- TOC entry 4810 (class 2606 OID 16502)
-- Name: shopping_carts shopping_carts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.shopping_carts
    ADD CONSTRAINT shopping_carts_pkey PRIMARY KEY (cart_id);


--
-- TOC entry 4812 (class 2606 OID 16509)
-- Name: support_messages support_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.support_messages
    ADD CONSTRAINT support_messages_pkey PRIMARY KEY (message_id);


--
-- TOC entry 4814 (class 2606 OID 16517)
-- Name: support_tickets support_tickets_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.support_tickets
    ADD CONSTRAINT support_tickets_pkey PRIMARY KEY (ticket_id);


--
-- TOC entry 4816 (class 2606 OID 16523)
-- Name: transactions transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (transaction_id);


--
-- TOC entry 4792 (class 2606 OID 16574)
-- Name: permission uk_2ojme20jpga3r4r79tdso17gi; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.permission
    ADD CONSTRAINT uk_2ojme20jpga3r4r79tdso17gi UNIQUE (name);


--
-- TOC entry 4806 (class 2606 OID 16576)
-- Name: roles uk_716hgxp60ym1lifrdgp67xt5k; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT uk_716hgxp60ym1lifrdgp67xt5k UNIQUE (role_name);


--
-- TOC entry 4780 (class 2606 OID 16572)
-- Name: inventories uk_93u7raebj7w49wn4449dv1v79; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inventories
    ADD CONSTRAINT uk_93u7raebj7w49wn4449dv1v79 UNIQUE (variant_id);


--
-- TOC entry 4818 (class 2606 OID 16580)
-- Name: user_account uk_castjbvpeeus0r8lbpehiu0e4; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_account
    ADD CONSTRAINT uk_castjbvpeeus0r8lbpehiu0e4 UNIQUE (username);


--
-- TOC entry 4774 (class 2606 OID 16570)
-- Name: coupons uk_eplt0kkm9yf2of2lnx6c1oy9b; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.coupons
    ADD CONSTRAINT uk_eplt0kkm9yf2of2lnx6c1oy9b UNIQUE (code);


--
-- TOC entry 4766 (class 2606 OID 16568)
-- Name: brands uk_gds2u6k2vfeo1tkrtgwcyqj36; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.brands
    ADD CONSTRAINT uk_gds2u6k2vfeo1tkrtgwcyqj36 UNIQUE (brand_name);


--
-- TOC entry 4820 (class 2606 OID 16578)
-- Name: user_account uk_hl02wv5hym99ys465woijmfib; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_account
    ADD CONSTRAINT uk_hl02wv5hym99ys465woijmfib UNIQUE (email);


--
-- TOC entry 4826 (class 2606 OID 16582)
-- Name: user_preferences uk_jmj1vdg3wyf01m2dyc4vhpcw4; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT uk_jmj1vdg3wyf01m2dyc4vhpcw4 UNIQUE (profile_id);


--
-- TOC entry 4830 (class 2606 OID 16584)
-- Name: user_profiles uk_opddmxm4ps5aqyvraslrkb1u1; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT uk_opddmxm4ps5aqyvraslrkb1u1 UNIQUE (account_id);


--
-- TOC entry 4822 (class 2606 OID 16528)
-- Name: user_account user_account_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_account
    ADD CONSTRAINT user_account_pkey PRIMARY KEY (account_id);


--
-- TOC entry 4824 (class 2606 OID 16536)
-- Name: user_addresses user_addresses_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_addresses
    ADD CONSTRAINT user_addresses_pkey PRIMARY KEY (address_id);


--
-- TOC entry 4828 (class 2606 OID 16543)
-- Name: user_preferences user_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT user_preferences_pkey PRIMARY KEY (preference_id);


--
-- TOC entry 4832 (class 2606 OID 16551)
-- Name: user_profiles user_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (profile_id);


--
-- TOC entry 4834 (class 2606 OID 16556)
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (account_id, role_id);


--
-- TOC entry 4836 (class 2606 OID 16561)
-- Name: wishlist_items wishlist_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_pkey PRIMARY KEY (wishlist_item_id);


--
-- TOC entry 4838 (class 2606 OID 16566)
-- Name: wishlists wishlists_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wishlists
    ADD CONSTRAINT wishlists_pkey PRIMARY KEY (wishlist_id);


--
-- TOC entry 4864 (class 2606 OID 16710)
-- Name: user_profiles fk50visa7oimxyh74es67rhtf82; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT fk50visa7oimxyh74es67rhtf82 FOREIGN KEY (account_id) REFERENCES public.user_account(account_id);


--
-- TOC entry 4847 (class 2606 OID 16625)
-- Name: payments fk81gagumt0r8y3rmudcgpbk42l; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk81gagumt0r8y3rmudcgpbk42l FOREIGN KEY (order_id) REFERENCES public.orders(order_id);


--
-- TOC entry 4843 (class 2606 OID 16605)
-- Name: notifications fk93ucg7hjxp167px7ekspfh896; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk93ucg7hjxp167px7ekspfh896 FOREIGN KEY (profile_id) REFERENCES public.user_profiles(profile_id);


--
-- TOC entry 4859 (class 2606 OID 16685)
-- Name: support_tickets fk9eru6tjf0e9384b1t2cflcjqb; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.support_tickets
    ADD CONSTRAINT fk9eru6tjf0e9384b1t2cflcjqb FOREIGN KEY (profile_id) REFERENCES public.user_profiles(profile_id);


--
-- TOC entry 4841 (class 2606 OID 16595)
-- Name: categories fk9il7y6fehxwunjeepq0n7g5rd; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT fk9il7y6fehxwunjeepq0n7g5rd FOREIGN KEY (parent_category_id) REFERENCES public.categories(category_id);


--
-- TOC entry 4850 (class 2606 OID 16640)
-- Name: products fka3a4mpsfdf4d2y6r8ra3sc8mv; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fka3a4mpsfdf4d2y6r8ra3sc8mv FOREIGN KEY (brand_id) REFERENCES public.brands(brand_id);


--
-- TOC entry 4857 (class 2606 OID 16675)
-- Name: support_messages fkaip2ievaaep0m4iestcg7tsbe; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.support_messages
    ADD CONSTRAINT fkaip2ievaaep0m4iestcg7tsbe FOREIGN KEY (profile_id) REFERENCES public.user_profiles(profile_id);


--
-- TOC entry 4844 (class 2606 OID 16615)
-- Name: order_items fkb0fe3pykpehuhrhkam7u57va5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fkb0fe3pykpehuhrhkam7u57va5 FOREIGN KEY (variant_id) REFERENCES public.product_variant(variant_id);


--
-- TOC entry 4845 (class 2606 OID 16610)
-- Name: order_items fkbioxgbv59vetrxe0ejfubep1w; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fkbioxgbv59vetrxe0ejfubep1w FOREIGN KEY (order_id) REFERENCES public.orders(order_id);


--
-- TOC entry 4856 (class 2606 OID 16670)
-- Name: shopping_carts fkceapu7ueyekrj5yg6ewc2j2y6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.shopping_carts
    ADD CONSTRAINT fkceapu7ueyekrj5yg6ewc2j2y6 FOREIGN KEY (profile_id) REFERENCES public.user_profiles(profile_id);


--
-- TOC entry 4858 (class 2606 OID 16680)
-- Name: support_messages fkejv4umpsfsqv4amvnjrmni3xi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.support_messages
    ADD CONSTRAINT fkejv4umpsfsqv4amvnjrmni3xi FOREIGN KEY (ticket_id) REFERENCES public.support_tickets(ticket_id);


--
-- TOC entry 4860 (class 2606 OID 16690)
-- Name: transactions fkfyxndk58yiq2vpn0yd4m09kbt; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT fkfyxndk58yiq2vpn0yd4m09kbt FOREIGN KEY (order_id) REFERENCES public.orders(order_id);


--
-- TOC entry 4854 (class 2606 OID 16660)
-- Name: role_permissions fkh0v7u4w7mttcu81o8wegayr8e; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkh0v7u4w7mttcu81o8wegayr8e FOREIGN KEY (permission_id) REFERENCES public.permission(permission_id);


--
-- TOC entry 4865 (class 2606 OID 16715)
-- Name: user_roles fkh8ciramu9cc9q3qcqiv4ue8a6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkh8ciramu9cc9q3qcqiv4ue8a6 FOREIGN KEY (role_id) REFERENCES public.roles(role_id);


--
-- TOC entry 4862 (class 2606 OID 16700)
-- Name: user_addresses fkh9r98weiqb224xhtw9fw918td; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_addresses
    ADD CONSTRAINT fkh9r98weiqb224xhtw9fw918td FOREIGN KEY (profile_id) REFERENCES public.user_profiles(profile_id);


--
-- TOC entry 4852 (class 2606 OID 16655)
-- Name: reviews fkhqbvvr9bf50683fphpef0xe38; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT fkhqbvvr9bf50683fphpef0xe38 FOREIGN KEY (profile_id) REFERENCES public.user_profiles(profile_id);


--
-- TOC entry 4869 (class 2606 OID 16735)
-- Name: wishlists fkif0ahf096oyesofe2wtn89dp3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wishlists
    ADD CONSTRAINT fkif0ahf096oyesofe2wtn89dp3 FOREIGN KEY (profile_id) REFERENCES public.user_profiles(profile_id);


--
-- TOC entry 4867 (class 2606 OID 16730)
-- Name: wishlist_items fkkem9l8vd14pk3cc4elnpl0n00; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT fkkem9l8vd14pk3cc4elnpl0n00 FOREIGN KEY (wishlist_id) REFERENCES public.wishlists(wishlist_id);


--
-- TOC entry 4863 (class 2606 OID 16705)
-- Name: user_preferences fkmo9r5b6dte1fwy4rwgyjvpk16; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT fkmo9r5b6dte1fwy4rwgyjvpk16 FOREIGN KEY (profile_id) REFERENCES public.user_profiles(profile_id);


--
-- TOC entry 4855 (class 2606 OID 16665)
-- Name: role_permissions fkn5fotdgk8d1xvo8nav9uv3muc; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.role_permissions
    ADD CONSTRAINT fkn5fotdgk8d1xvo8nav9uv3muc FOREIGN KEY (role_id) REFERENCES public.roles(role_id);


--
-- TOC entry 4866 (class 2606 OID 16720)
-- Name: user_roles fkn80g7pr45rjngqdg3tq9n28r8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkn80g7pr45rjngqdg3tq9n28r8 FOREIGN KEY (account_id) REFERENCES public.user_account(account_id);


--
-- TOC entry 4851 (class 2606 OID 16645)
-- Name: products fkog2rp4qthbtt2lfyhfo32lsw9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fkog2rp4qthbtt2lfyhfo32lsw9 FOREIGN KEY (category_id) REFERENCES public.categories(category_id);


--
-- TOC entry 4839 (class 2606 OID 16585)
-- Name: cart_items fkojy3ibx281qswho045bw4q0da; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT fkojy3ibx281qswho045bw4q0da FOREIGN KEY (cart_id) REFERENCES public.shopping_carts(cart_id);


--
-- TOC entry 4861 (class 2606 OID 16695)
-- Name: transactions fkp162mo7k0gjno4cf79tsk9b48; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT fkp162mo7k0gjno4cf79tsk9b48 FOREIGN KEY (profile_id) REFERENCES public.user_profiles(profile_id);


--
-- TOC entry 4842 (class 2606 OID 16600)
-- Name: inventories fkpfvaqa1f7gx2iakypg58y3aw8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.inventories
    ADD CONSTRAINT fkpfvaqa1f7gx2iakypg58y3aw8 FOREIGN KEY (variant_id) REFERENCES public.product_variant(variant_id);


--
-- TOC entry 4853 (class 2606 OID 16650)
-- Name: reviews fkpl51cejpw4gy5swfar8br9ngi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT fkpl51cejpw4gy5swfar8br9ngi FOREIGN KEY (product_id) REFERENCES public.products(product_id);


--
-- TOC entry 4846 (class 2606 OID 16620)
-- Name: orders fkput6dwfq51b1d7ekn7ksgvyig; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fkput6dwfq51b1d7ekn7ksgvyig FOREIGN KEY (user_id) REFERENCES public.user_profiles(profile_id);


--
-- TOC entry 4848 (class 2606 OID 16630)
-- Name: product_images fkqnq71xsohugpqwf3c9gxmsuy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_images
    ADD CONSTRAINT fkqnq71xsohugpqwf3c9gxmsuy FOREIGN KEY (product_id) REFERENCES public.products(product_id);


--
-- TOC entry 4868 (class 2606 OID 16725)
-- Name: wishlist_items fkqxj7lncd242b59fb78rqegyxj; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT fkqxj7lncd242b59fb78rqegyxj FOREIGN KEY (product_id) REFERENCES public.products(product_id);


--
-- TOC entry 4840 (class 2606 OID 16590)
-- Name: cart_items fks6mhglhr70icpojokqawivvmm; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT fks6mhglhr70icpojokqawivvmm FOREIGN KEY (variant_id) REFERENCES public.product_variant(variant_id);


--
-- TOC entry 4849 (class 2606 OID 16635)
-- Name: product_variant fktk6wrh1xwqq4vn2pf11mwycgv; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_variant
    ADD CONSTRAINT fktk6wrh1xwqq4vn2pf11mwycgv FOREIGN KEY (product_id) REFERENCES public.products(product_id);


-- Completed on 2025-10-02 03:08:17

--
-- PostgreSQL database dump complete
--

\unrestrict 21czpZD7fq3TAWL3Ew2L6lhTkv9o37339ZgfYczO7Gqy34odAtbB5tS4mtBe33a

