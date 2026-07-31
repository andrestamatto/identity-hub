alter table application_client
    alter column audience drop not null,
    drop constraint application_client_type_check;

alter table application_client
    add constraint application_client_type_check check (client_type in ('API', 'SPA')),
    add constraint application_client_settings_check check (
        (client_type = 'API' and audience is not null)
        or (client_type = 'SPA' and audience is null)
    );

create table application_client_spa_redirect_uri (
    application_client_id uuid not null references application_client (id) on delete cascade,
    position smallint not null,
    redirect_uri varchar(2048) not null,
    primary key (application_client_id, position),
    constraint application_client_spa_redirect_unique
        unique (application_client_id, redirect_uri),
    constraint application_client_spa_redirect_position_check
        check (position between 0 and 9)
);

create table application_client_spa_web_origin (
    application_client_id uuid not null references application_client (id) on delete cascade,
    position smallint not null,
    web_origin varchar(255) not null,
    primary key (application_client_id, position),
    constraint application_client_spa_origin_unique
        unique (application_client_id, web_origin),
    constraint application_client_spa_origin_position_check
        check (position between 0 and 9)
);
