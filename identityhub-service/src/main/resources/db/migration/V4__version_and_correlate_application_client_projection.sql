alter table application_client_projection_outbox
    add column payload_version smallint not null default 1,
    add column correlation_id varchar(64);

update application_client_projection_outbox
set correlation_id = 'migration-v4'
where correlation_id is null;

alter table application_client_projection_outbox
    alter column payload_version drop default,
    alter column correlation_id set not null,
    add constraint application_client_projection_payload_version_check
        check (payload_version = 1),
    add constraint application_client_projection_correlation_format_check
        check (correlation_id ~ '^[A-Za-z0-9._-]{1,64}$');
