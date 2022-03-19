--
-- PostgreSQL database dump
--

-- Dumped from database version 13.4
-- Dumped by pg_dump version 13.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
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
-- Name: account; Type: TABLE; Schema: public; Owner: redcrab
--

CREATE TABLE public.account (
    login character varying(255),
    password character varying(255),
    id integer NOT NULL
);


ALTER TABLE public.account OWNER TO redcrab;

--
-- Name: account_id_seq; Type: SEQUENCE; Schema: public; Owner: redcrab
--

CREATE SEQUENCE public.account_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.account_id_seq OWNER TO redcrab;

--
-- Name: account_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: redcrab
--

ALTER SEQUENCE public.account_id_seq OWNED BY public.account.id;


--
-- Name: files; Type: TABLE; Schema: public; Owner: redcrab
--

CREATE TABLE public.files (
    idnotes integer,
    name character varying(255)
);


ALTER TABLE public.files OWNER TO redcrab;

--
-- Name: notes; Type: TABLE; Schema: public; Owner: redcrab
--

CREATE TABLE public.notes (
    iduser integer NOT NULL,
    name character varying(255),
    text character varying(1000),
    label character varying(40) DEFAULT 'default'::character varying,
    status boolean,
    id integer NOT NULL
);


ALTER TABLE public.notes OWNER TO redcrab;

--
-- Name: notes_id_seq; Type: SEQUENCE; Schema: public; Owner: redcrab
--

CREATE SEQUENCE public.notes_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.notes_id_seq OWNER TO redcrab;

--
-- Name: notes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: redcrab
--

ALTER SEQUENCE public.notes_id_seq OWNED BY public.notes.id;


--
-- Name: account id; Type: DEFAULT; Schema: public; Owner: redcrab
--

ALTER TABLE ONLY public.account ALTER COLUMN id SET DEFAULT nextval('public.account_id_seq'::regclass);


--
-- Name: notes id; Type: DEFAULT; Schema: public; Owner: redcrab
--

ALTER TABLE ONLY public.notes ALTER COLUMN id SET DEFAULT nextval('public.notes_id_seq'::regclass);


--
-- Data for Name: account; Type: TABLE DATA; Schema: public; Owner: redcrab
--

COPY public.account (login, password, id) FROM stdin;
\.


--
-- Data for Name: files; Type: TABLE DATA; Schema: public; Owner: redcrab
--

COPY public.files (idnotes, name) FROM stdin;
\.


--
-- Data for Name: notes; Type: TABLE DATA; Schema: public; Owner: redcrab
--

COPY public.notes (iduser, name, text, label, status, id) FROM stdin;
\.


--
-- Name: account_id_seq; Type: SEQUENCE SET; Schema: public; Owner: redcrab
--

SELECT pg_catalog.setval('public.account_id_seq', 44, true);


--
-- Name: notes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: redcrab
--

SELECT pg_catalog.setval('public.notes_id_seq', 36, true);


--
-- Name: account constraintname; Type: CONSTRAINT; Schema: public; Owner: redcrab
--

ALTER TABLE ONLY public.account
    ADD CONSTRAINT constraintname UNIQUE (login);


--
-- PostgreSQL database dump complete
--

