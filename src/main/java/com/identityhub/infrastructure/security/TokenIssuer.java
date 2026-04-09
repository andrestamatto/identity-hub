package com.identityhub.infrastructure.security;

import com.identityhub.domain.model.User;

public interface TokenIssuer {
    String issue(User user);
}
