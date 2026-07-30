package br.dev.andrestamatto.identityhub.clientapplication.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ApplicationIdentifierTest {

    @Test
    void acceptsLowercaseSlugWithinSupportedLength() {
        var identifier = new ApplicationIdentifier("auto-radar");

        assertThat(identifier.value()).isEqualTo("auto-radar");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        " ",
        "ab",
        "Auto-Radar",
        "auto_radar",
        "-auto-radar",
        "auto-radar-",
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    })
    void rejectsUnsupportedIdentifier(String value) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ApplicationIdentifier(value));
    }
}
