package br.dev.andrestamatto.identityhub.domain.model;

import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.Map;

public record SocialIdentity(
        SocialProvider provider,
        String providerUserId,
        @Nullable String email,
        Map<String, Object> attributes
) {

    @Getter
    public static class Builder {

        private SocialProvider provider;
        private String providerUserId;
        private @Nullable String email;
        private Map<String, Object> attributes;

        public Builder provider(SocialProvider provider) { this.provider = provider; return this; }
        public Builder providerUserId(String providerUserId) { this.providerUserId = providerUserId; return this; }
        public Builder email(@Nullable String email) { this.email = email; return this; }
        public Builder attributes(Map<String, Object> attributes) { this.attributes = attributes; return this; }

        public SocialIdentity build() {
            return new SocialIdentity(
                    this.provider,
                    this.providerUserId,
                    this.email,
                    this.attributes
            );
        }
    }
}
