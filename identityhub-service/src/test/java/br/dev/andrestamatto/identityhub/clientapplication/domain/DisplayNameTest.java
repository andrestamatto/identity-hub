package br.dev.andrestamatto.identityhub.clientapplication.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DisplayNameTest {

    @Test
    void removesSurroundingWhitespace() {
        var displayName = new DisplayName("  Auto Radar  ");

        assertThat(displayName.value()).isEqualTo("Auto Radar");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsMissingDisplayName(String value) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DisplayName(value));
    }

    @Test
    void rejectsDisplayNameLongerThanSupported() {
        var unsupportedName = "a".repeat(121);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DisplayName(unsupportedName));
    }
}
