alter table application_client
    drop constraint application_client_type_check,
    drop constraint application_client_settings_check;

alter table application_client
    add constraint application_client_type_check
        check (client_type in ('API', 'SPA', 'BFF', 'MACHINE')),
    add constraint application_client_settings_check check (
        (client_type = 'API' and audience is not null)
        or (client_type in ('SPA', 'BFF', 'MACHINE') and audience is null)
    );
