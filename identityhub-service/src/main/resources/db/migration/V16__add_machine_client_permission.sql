create table application_client_machine_permission (
    application_client_id uuid not null
        references application_client (id) on delete cascade,
    position smallint not null,
    scope varchar(63) not null,
    primary key (application_client_id, position),
    constraint application_client_machine_permission_unique
        unique (application_client_id, scope),
    constraint application_client_machine_permission_format_check
        check (scope in ('membership:write'))
);
