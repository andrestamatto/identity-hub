package br.dev.andrestamatto.identityhub.clientapplication.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class ApplicationClientKeyTest {

    @Test
    void acceptsLowercaseSlug() {
        assertThat(new ApplicationClientKey("social-catalog-api").value())
                .isEqualTo("social-catalog-api");
    }

    @Test
    void rejectsUnsupportedValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationClientKey(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationClientKey("ab"));
        assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationClientKey("API-main"));
        assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationClientKey("api_*"));
    }
}
