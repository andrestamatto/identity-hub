create table client_application (
    id uuid primary key,
    identifier varchar(63) not null,
    display_name varchar(120) not null,
    state varchar(32) not null,
    registered_at timestamp with time zone not null,
    constraint client_application_identifier_unique unique (identifier),
    constraint client_application_identifier_format_check
        check (identifier ~ '^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])$'),
    constraint client_application_display_name_check
        check (
            char_length(display_name) between 1 and 120
            and display_name = btrim(display_name)
        ),
    constraint client_application_state_check
        check (state in ('DRAFT'))
);
