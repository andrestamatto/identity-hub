create table onboarding_session (
    id varchar(43) primary key,
    application_id uuid not null references client_application (id),
    machine_client_id uuid not null references application_client (id),
    browser_client_id uuid not null references application_client (id),
    acquisition_reference_digest char(64) not null,
    redirect_uri varchar(2048) not null,
    pkce_code_challenge char(43) not null,
    idempotency_key_digest char(64) not null,
    request_digest char(64) not null,
    correlation_id varchar(64) not null,
    state varchar(16) not null,
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    constraint onboarding_session_idempotency_unique
        unique (machine_client_id, idempotency_key_digest),
    constraint onboarding_session_id_format_check
        check (id ~ '^[A-Za-z0-9_-]{43}$'),
    constraint onboarding_session_acquisition_digest_check
        check (acquisition_reference_digest ~ '^[0-9a-f]{64}$'),
    constraint onboarding_session_idempotency_digest_check
        check (idempotency_key_digest ~ '^[0-9a-f]{64}$'),
    constraint onboarding_session_request_digest_check
        check (request_digest ~ '^[0-9a-f]{64}$'),
    constraint onboarding_session_pkce_check
        check (pkce_code_challenge ~ '^[A-Za-z0-9_-]{43}$'),
    constraint onboarding_session_state_check
        check (state in ('PENDING')),
    constraint onboarding_session_expiration_check
        check (expires_at > created_at)
);

create index onboarding_session_expiration_idx
    on onboarding_session (expires_at);
