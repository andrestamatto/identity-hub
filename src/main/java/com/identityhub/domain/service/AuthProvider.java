package com.identityhub.domain.service;

import com.identityhub.domain.model.User;

public interface AuthProvider {
    User authenticate(String email, String password);
}
