package com.baytalhikmah.sovereign.domain;

import java.util.Set;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SignupChannelValidator implements ConstraintValidator<SignupChannel, String> {

    private static final Set<String> HUMAN_CHANNELS = Set.of("web", "mobile", "store");

    private static final Pattern AGENT_CHANNEL = Pattern.compile("agent:[a-z0-9-]{3,40}");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return HUMAN_CHANNELS.contains(value) || AGENT_CHANNEL.matcher(value).matches();
    }

}
