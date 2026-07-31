create table application_client (
    id uuid primary key,
    application_id uuid not null references client_application (id),
    client_key varchar(63) not null,
    client_type varchar(16) not null,
    audience varchar(255) not null,
    enabled boolean not null,
    configured_at timestamp with time zone not null,
    constraint application_client_key_unique unique (application_id, client_key),
    constraint application_client_audience_unique unique (audience),
    constraint application_client_key_format_check
        check (client_key ~ '^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])$'),
    constraint application_client_type_check check (client_type in ('API')),
    constraint application_client_audience_format_check
        check (audience ~ '^[A-Za-z0-9][A-Za-z0-9._:/-]*[A-Za-z0-9]$')
);

create table application_client_projection_outbox (
    operation_id uuid primary key,
    application_client_id uuid not null unique references application_client (id),
    state varchar(16) not null,
    attempts integer not null,
    next_attempt_at timestamp with time zone not null,
    last_failure_code varchar(64),
    locked_by uuid,
    locked_until timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint application_client_projection_state_check
        check (state in ('PENDING', 'APPLIED', 'FAILED')),
    constraint application_client_projection_attempts_check check (attempts >= 0),
    constraint application_client_projection_lock_check
        check (
            (locked_by is null and locked_until is null)
            or (locked_by is not null and locked_until is not null)
        )
);

create index application_client_projection_due_idx
    on application_client_projection_outbox (state, next_attempt_at)
    where state = 'PENDING';
