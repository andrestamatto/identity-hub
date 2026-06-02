package br.dev.andrestamatto.identityhub.infrastructure.messaging.templates;

import java.util.Map;

public interface EmailTemplate {
    String create(String recipient, Map<String, String> templateDetails);
}
