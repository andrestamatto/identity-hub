create table membership (
    id uuid primary key,
    application_id uuid not null references client_application (id),
    user_account_ref uuid not null,
    state varchar(16) not null,
    requested_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint membership_application_user_unique
        unique (application_id, user_account_ref),
    constraint membership_state_check
        check (state in ('PENDING'))
);

create table membership_grant_operation (
    operation_id uuid primary key,
    membership_id uuid not null references membership (id),
    application_client_id uuid not null references application_client (id),
    idempotency_key varchar(128) not null unique,
    command_fingerprint char(64) not null,
    correlation_id varchar(64) not null,
    accepted_at timestamp with time zone not null,
    constraint membership_grant_key_check
        check (char_length(btrim(idempotency_key)) between 8 and 128),
    constraint membership_grant_fingerprint_check
        check (command_fingerprint ~ '^[0-9a-f]{64}$'),
    constraint membership_grant_correlation_check
        check (char_length(btrim(correlation_id)) between 1 and 64)
);

create index membership_user_idx
    on membership (user_account_ref, application_id);

create index membership_grant_membership_idx
    on membership_grant_operation (membership_id, accepted_at desc);
