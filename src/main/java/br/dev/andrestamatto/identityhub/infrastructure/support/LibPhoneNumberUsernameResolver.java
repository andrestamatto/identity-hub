package br.dev.andrestamatto.identityhub.infrastructure.support;

import br.dev.andrestamatto.identityhub.application.ports.output.UsernameResolver;
import br.dev.andrestamatto.identityhub.domain.valueobjects.Username;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.util.Locale;
import java.util.regex.Pattern;

public class LibPhoneNumberUsernameResolver implements UsernameResolver {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@([\\w-]+\\.)+[\\w-]{2,}$");

    private final PhoneNumberUtil phoneNumberUtil;
    private final String defaultPhoneRegion;

    public LibPhoneNumberUsernameResolver(PhoneNumberUtil phoneNumberUtil, String defaultPhoneRegion) {
        this.phoneNumberUtil = phoneNumberUtil;
        this.defaultPhoneRegion = defaultPhoneRegion;
    }

    @Override
    public Username resolve(String rawUsername) {
        if (isEmail(rawUsername)) {
            return Username.email(normalizeEmail(rawUsername));
        }

        if (isPhone(rawUsername)) {
            return Username.phone(normalizePhone(rawUsername));
        }

        throw new IllegalArgumentException("Invalid username value.");
    }

    private boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value.trim()).matches();
    }

    private boolean isPhone(String value) {
        try {
            var phoneNumber = phoneNumberUtil.parse(value, defaultPhoneRegion);
            return phoneNumberUtil.isValidNumber(phoneNumber);
        } catch (NumberParseException exception) {
            return false;
        }
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String value) {
        try {
            var phoneNumber = phoneNumberUtil.parse(value, defaultPhoneRegion);
            return phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException exception) {
            throw new IllegalArgumentException("Invalid phone username value.", exception);
        }
    }
}
