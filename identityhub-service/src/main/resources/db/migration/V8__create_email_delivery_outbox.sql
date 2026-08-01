create table email_delivery_outbox (
    delivery_id uuid primary key,
    application_id uuid not null references client_application (id),
    application_identifier varchar(63) not null,
    application_display_name varchar(120) not null,
    environment varchar(32) not null,
    recipient varchar(254) not null,
    purpose varchar(32) not null,
    state varchar(16) not null,
    attempts integer not null,
    next_attempt_at timestamp with time zone not null,
    last_failure_code varchar(64),
    correlation_id varchar(128) not null,
    locked_by uuid,
    locked_until timestamp with time zone,
    requested_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint email_delivery_purpose_check
        check (purpose in ('PASSWORD_CHANGED')),
    constraint email_delivery_state_check
        check (state in ('PENDING', 'DELIVERED', 'FAILED')),
    constraint email_delivery_attempts_check check (attempts >= 0),
    constraint email_delivery_recipient_check
        check (char_length(recipient) between 3 and 254 and recipient = btrim(recipient)),
    constraint email_delivery_lease_check
        check ((locked_by is null) = (locked_until is null))
);

create index email_delivery_due_idx
    on email_delivery_outbox (next_attempt_at, requested_at)
    where state = 'PENDING';
