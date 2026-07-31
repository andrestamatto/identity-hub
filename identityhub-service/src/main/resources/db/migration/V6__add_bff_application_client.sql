alter table application_client
    drop constraint application_client_type_check,
    drop constraint application_client_settings_check;

alter table application_client
    add constraint application_client_type_check check (client_type in ('API', 'SPA', 'BFF')),
    add constraint application_client_settings_check check (
        (client_type = 'API' and audience is not null)
        or (client_type in ('SPA', 'BFF') and audience is null)
    );

alter table application_client_spa_redirect_uri
    rename to application_client_browser_redirect_uri;

alter table application_client_browser_redirect_uri
    rename constraint application_client_spa_redirect_unique
        to application_client_browser_redirect_unique;

alter table application_client_browser_redirect_uri
    rename constraint application_client_spa_redirect_position_check
        to application_client_browser_redirect_position_check;
