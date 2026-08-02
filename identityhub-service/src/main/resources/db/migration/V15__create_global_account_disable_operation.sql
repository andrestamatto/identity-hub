create table global_account_disable_operation (
    operation_id uuid primary key,
    user_account_ref uuid not null,
    reason varchar(500) not null,
    idempotency_key varchar(128) not null unique,
    command_fingerprint char(64) not null,
    actor_subject varchar(255) not null,
    correlation_id varchar(64) not null,
    status varchar(16) not null,
    rejection varchar(64),
    requested_at timestamp with time zone not null,
    completed_at timestamp with time zone,
    updated_at timestamp with time zone not null,
    constraint global_account_disable_reason_check
        check (char_length(btrim(reason)) between 10 and 500),
    constraint global_account_disable_key_check
        check (char_length(btrim(idempotency_key)) between 8 and 128),
    constraint global_account_disable_fingerprint_check
        check (command_fingerprint ~ '^[0-9a-f]{64}$'),
    constraint global_account_disable_status_check
        check (status in ('PENDING', 'COMPLETED', 'REJECTED', 'FAILED')),
    constraint global_account_disable_rejection_check
        check (
            (status = 'REJECTED' and rejection is not null and completed_at is not null)
            or (status = 'COMPLETED' and rejection is null and completed_at is not null)
            or (status = 'FAILED' and rejection is null and completed_at is not null)
            or (status = 'PENDING' and rejection is null and completed_at is null)
        )
);

create index global_account_disable_target_idx
    on global_account_disable_operation (user_account_ref, requested_at desc);

create index global_account_disable_status_idx
    on global_account_disable_operation (status, updated_at);
