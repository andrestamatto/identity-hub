alter table membership
    add column activated_at timestamp with time zone;

alter table membership
    drop constraint membership_state_check;

alter table membership
    add constraint membership_state_check check (
        (state = 'PENDING' and activated_at is null)
        or (state = 'ACTIVE' and activated_at is not null)
    );

create table membership_projection_outbox (
    membership_id uuid primary key references membership (id) on delete cascade,
    payload_version integer not null,
    correlation_id varchar(64) not null,
    state varchar(16) not null,
    attempts integer not null default 0,
    next_attempt_at timestamp with time zone not null,
    last_failure_code varchar(64),
    lease_owner uuid,
    lease_until timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint membership_projection_payload_check check (payload_version = 1),
    constraint membership_projection_state_check
        check (state in ('PENDING', 'APPLIED', 'FAILED')),
    constraint membership_projection_attempts_check check (attempts >= 0),
    constraint membership_projection_correlation_check
        check (char_length(btrim(correlation_id)) between 1 and 64),
    constraint membership_projection_lease_check check (
        (lease_owner is null and lease_until is null)
        or (lease_owner is not null and lease_until is not null)
    )
);

create index membership_projection_due_idx
    on membership_projection_outbox (state, next_attempt_at, lease_until);

insert into membership_projection_outbox (
    membership_id, payload_version, correlation_id, state, attempts,
    next_attempt_at, created_at, updated_at
)
select m.id,
       1,
       coalesce((
           select o.correlation_id
           from membership_grant_operation o
           where o.membership_id = m.id
           order by o.accepted_at, o.operation_id
           limit 1
       ), 'migration-v18'),
       'PENDING',
       0,
       m.requested_at,
       m.requested_at,
       m.updated_at
from membership m
where m.state = 'PENDING';
