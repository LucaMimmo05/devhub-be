-- Baseline: the schema Hibernate generated from the entities as of October 2026
-- (pg_dump --schema-only, verified equivalent to the dev database).
-- Existing databases skip this file (quarkus.flyway.baseline-on-migrate, baseline-version=1);
-- it only runs on an empty database.

CREATE TABLE public.commands (
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    id uuid NOT NULL,
    user_profile_id uuid NOT NULL,
    category character varying(50) NOT NULL,
    title character varying(100) NOT NULL,
    description character varying(500),
    command text NOT NULL
);

CREATE TABLE public.github_connections (
    created_at timestamp(6) with time zone,
    github_id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    id uuid NOT NULL,
    user_profile_id uuid NOT NULL,
    github_login character varying(100) NOT NULL,
    github_avatar_url character varying(500),
    access_token text NOT NULL,
    github_name character varying(255),
    scopes character varying(255)
);

CREATE TABLE public.notes (
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    emoji character varying(16),
    id uuid NOT NULL,
    user_profile_id uuid NOT NULL,
    title character varying(100) NOT NULL,
    content text
);

CREATE TABLE public.otp_codes (
    attempts integer DEFAULT 0 NOT NULL,
    created_at timestamp(6) with time zone,
    expires_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    type character varying(50) NOT NULL,
    code_hash character varying(255) NOT NULL,
    email character varying(255) NOT NULL
);

CREATE TABLE public.project_members (
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    id uuid NOT NULL,
    project_id uuid NOT NULL,
    user_profile_id uuid NOT NULL,
    role character varying(255) NOT NULL,
    CONSTRAINT project_members_role_check CHECK (((role)::text = ANY ((ARRAY['OWNER'::character varying, 'MEMBER'::character varying])::text[])))
);

CREATE TABLE public.projects (
    created_at timestamp(6) with time zone,
    due_date timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    id uuid NOT NULL,
    owner_id uuid NOT NULL,
    title character varying(25) NOT NULL,
    github_repo character varying(150),
    description character varying(255) NOT NULL,
    img_url character varying(255),
    priority character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT projects_priority_check CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying])::text[]))),
    CONSTRAINT projects_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'ARCHIVED'::character varying])::text[])))
);

CREATE TABLE public.refresh_tokens (
    created_at timestamp(6) with time zone,
    expires_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    token character varying(255) NOT NULL
);

CREATE TABLE public.tasks (
    created_at timestamp(6) with time zone,
    due_date timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    assigned_to_id uuid,
    created_by_id uuid,
    id uuid NOT NULL,
    project_id uuid,
    title character varying(100) NOT NULL,
    description character varying(500),
    priority character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT tasks_priority_check CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying])::text[]))),
    CONSTRAINT tasks_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'ARCHIVED'::character varying])::text[])))
);

CREATE TABLE public.user_profiles (
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    username character varying(50),
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    avatar_url character varying(255)
);

CREATE TABLE public.users (
    is_active boolean,
    is_email_verified boolean,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    id uuid NOT NULL,
    username character varying(50) NOT NULL,
    email character varying(100) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying])::text[])))
);

ALTER TABLE ONLY public.commands
    ADD CONSTRAINT commands_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.github_connections
    ADD CONSTRAINT github_connections_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.github_connections
    ADD CONSTRAINT github_connections_user_profile_id_key UNIQUE (user_profile_id);

ALTER TABLE ONLY public.notes
    ADD CONSTRAINT notes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.otp_codes
    ADD CONSTRAINT otp_codes_email_type_key UNIQUE (email, type);

ALTER TABLE ONLY public.otp_codes
    ADD CONSTRAINT otp_codes_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.project_members
    ADD CONSTRAINT project_members_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.projects
    ADD CONSTRAINT projects_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_key UNIQUE (token);

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT tasks_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_user_id_key UNIQUE (user_id);

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_username_key UNIQUE (username);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.projects
    ADD CONSTRAINT fkafijmo1di7shfa6aixiq2bqgo FOREIGN KEY (owner_id) REFERENCES public.user_profiles(id);

ALTER TABLE ONLY public.commands
    ADD CONSTRAINT fkd2e9hpewyks7bpnayrquv6bft FOREIGN KEY (user_profile_id) REFERENCES public.user_profiles(id);

ALTER TABLE ONLY public.project_members
    ADD CONSTRAINT fkdki1sp2homqsdcvqm9yrix31g FOREIGN KEY (project_id) REFERENCES public.projects(id);

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT fkjcad5nfve11khsnpwj1mv8frj FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.project_members
    ADD CONSTRAINT fkl78yh84f2fgrchk66tvyw54xc FOREIGN KEY (user_profile_id) REFERENCES public.user_profiles(id);

ALTER TABLE ONLY public.notes
    ADD CONSTRAINT fkltqebijf69kt1hd2bv2gpl067 FOREIGN KEY (user_profile_id) REFERENCES public.user_profiles(id);

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT fkmv7l52s3ns2ly8xtrj4sl2pq4 FOREIGN KEY (assigned_to_id) REFERENCES public.user_profiles(id);

ALTER TABLE ONLY public.github_connections
    ADD CONSTRAINT fkn1dl4onblay2oligylomdujtr FOREIGN KEY (user_profile_id) REFERENCES public.user_profiles(id);

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT fkp36kmcpu1c0cngb0hnaspciak FOREIGN KEY (created_by_id) REFERENCES public.user_profiles(id);

ALTER TABLE ONLY public.tasks
    ADD CONSTRAINT fksfhn82y57i3k9uxww1s007acc FOREIGN KEY (project_id) REFERENCES public.projects(id);
