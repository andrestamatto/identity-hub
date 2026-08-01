alter table client_application
    add column self_registration_policy varchar(16) not null default 'DISABLED';

alter table client_application
    add constraint client_application_self_registration_policy_check
        check (self_registration_policy in ('DISABLED', 'ENABLED'));
