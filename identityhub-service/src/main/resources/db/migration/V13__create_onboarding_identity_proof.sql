alter table onboarding_session
    drop constraint onboarding_session_state_check;

alter table onboarding_session
    add column proof_issued_at timestamp with time zone,
    add constraint onboarding_session_state_check
        check (state in ('PENDING', 'PROOF_ISSUED')),
    add constraint onboarding_session_proof_state_check
        check ((state = 'PENDING' and proof_issued_at is null)
            or (state = 'PROOF_ISSUED' and proof_issued_at is not null));

create table onboarding_identity_proof (
    proof_digest char(64) primary key,
    onboarding_session_id varchar(43) not null unique
        references onboarding_session (id),
    user_account_ref uuid not null,
    application_id uuid not null references client_application (id),
    acquisition_reference_digest char(64) not null,
    correlation_id varchar(64) not null,
    email_verified boolean not null,
    state varchar(16) not null,
    issued_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    constraint onboarding_proof_digest_check
        check (proof_digest ~ '^[0-9a-f]{64}$'),
    constraint onboarding_proof_acquisition_digest_check
        check (acquisition_reference_digest ~ '^[0-9a-f]{64}$'),
    constraint onboarding_proof_email_verified_check
        check (email_verified),
    constraint onboarding_proof_state_check
        check (state in ('AVAILABLE')),
    constraint onboarding_proof_expiration_check
        check (expires_at > issued_at)
);

create index onboarding_identity_proof_expiration_idx
    on onboarding_identity_proof (expires_at);
