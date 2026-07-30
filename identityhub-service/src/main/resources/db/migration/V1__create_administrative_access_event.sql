create table administrative_access_event (
    id uuid primary key,
    occurred_at timestamp with time zone not null,
    correlation_id varchar(64) not null,
    actor_subject varchar(255),
    http_method varchar(16) not null,
    request_path varchar(512) not null,
    outcome varchar(16) not null,
    reason varchar(64) not null,
    constraint administrative_access_event_outcome_check
        check (outcome in ('ALLOWED', 'DENIED'))
);

create index administrative_access_event_occurred_at_idx
    on administrative_access_event (occurred_at);
