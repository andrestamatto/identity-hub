alter table application_client_machine_scope
    drop constraint application_client_machine_scope_format_check;

alter table application_client_machine_scope
    add constraint application_client_machine_scope_format_check
        check (scope in ('onboarding:write', 'membership:write'));
