create table password_recovery_challenge (
    challenge_id uuid primary key,
    user_account_ref uuid not null,
    application_id uuid not null references client_application (id),
    normalized_email varchar(254) not null,
    secret_digest bytea not null,
    state varchar(16) not null,
    attempts integer not null,
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    updated_at timestamp with time zone not null,
    constraint password_recovery_digest_check check (octet_length(secret_digest) = 32),
    constraint password_recovery_email_check
        check (normalized_email = lower(btrim(normalized_email)) and position('@' in normalized_email) > 1),
    constraint password_recovery_state_check
        check (state in ('ACTIVE', 'USED', 'SUPERSEDED', 'EXPIRED', 'FAILED')),
    constraint password_recovery_attempts_check check (attempts between 0 and 5),
    constraint password_recovery_expiry_check check (expires_at > created_at),
    constraint password_recovery_used_check
        check ((state = 'USED') = (used_at is not null))
);

create unique index password_recovery_one_active_idx
    on password_recovery_challenge (user_account_ref)
    where state = 'ACTIVE';

create index password_recovery_recent_idx
    on password_recovery_challenge (user_account_ref, created_at desc);

alter table email_delivery_outbox
    drop constraint email_delivery_purpose_check;

alter table email_delivery_outbox
    drop constraint email_delivery_sensitive_content_check;

alter table email_delivery_outbox
    add constraint email_delivery_purpose_check
        check (purpose in ('PASSWORD_CHANGED', 'EMAIL_VERIFICATION', 'PASSWORD_RECOVERY'));

alter table email_delivery_outbox
    add constraint email_delivery_sensitive_content_check
        check (
            (purpose = 'PASSWORD_CHANGED' and sensitive_content is null)
            or
            (purpose in ('EMAIL_VERIFICATION', 'PASSWORD_RECOVERY') and (
                (state = 'PENDING' and sensitive_content is not null)
                or (state in ('DELIVERED', 'FAILED') and sensitive_content is null)
            ))
        );
