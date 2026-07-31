package br.dev.andrestamatto.identityhub.clientapplication.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class TokenAudienceTest {

    @Test
    void acceptsExactOpaqueAudience() {
        assertThat(new TokenAudience("https://api.example.test/catalog").value())
                .isEqualTo("https://api.example.test/catalog");
    }

    @Test
    void rejectsBlankWildcardAndUnsafeValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenAudience(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenAudience("  "));
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenAudience("catalog-*"));
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenAudience("catalog api"));
        assertThatIllegalArgumentException().isThrownBy(() -> new TokenAudience("catalog?env=dev"));
    }
}
