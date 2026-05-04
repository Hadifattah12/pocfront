package com.test1.test1.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public class PhoneNumberUtils {

    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    public static String normalize(String phoneNumber, String region) {

        String defaultRegion = (region == null || region.isBlank()) ? "LB" : region;

        try {
            Phonenumber.PhoneNumber number;

            // If number starts with + → ignore region
            if (phoneNumber != null && phoneNumber.trim().startsWith("+")) {
                number = phoneUtil.parse(phoneNumber, null);
            } else {
                number = phoneUtil.parse(phoneNumber, defaultRegion);
            }
//            check length and general structure
            if (!phoneUtil.isPossibleNumber(number)) {
                throw new IllegalArgumentException("INVALID_PHONE_NUMBER");
            }

            return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);

        } catch (NumberParseException e) {

            if (e.getErrorType() == NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
                throw new IllegalArgumentException("INVALID_COUNTRY_CODE", e);
            }

            throw new IllegalArgumentException("INVALID_PHONE_NUMBER", e);
        }
    }
}