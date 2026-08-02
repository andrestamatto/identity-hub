package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

final class RecentAdminAuthenticationRequiredException extends RuntimeException {

    RecentAdminAuthenticationRequiredException() {
        super("Recent administrative authentication is required");
    }
}
