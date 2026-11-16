package com.baytalhikmah.sovereign.audit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Replaces the message (%m) of every log line with a masked copy: e-mail addresses keep their first letter and
 * their domain, IBANs keep their first and last four characters. Personal data never reaches the log output.
 */
public class MaskingMessageConverter extends ClassicConverter {

    private static final Pattern EMAIL =
        Pattern.compile("\\b([A-Za-z0-9])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");

    private static final Pattern IBAN = Pattern.compile("\\b([A-Z]{2}[0-9]{2})[A-Z0-9]{7,26}([A-Z0-9]{4})\\b");

    @Override
    public String convert(ILoggingEvent event) {
        return mask(event.getFormattedMessage());
    }

    public static String mask(String message) {
        if (message == null) {
            return null;
        }
        String masked = EMAIL.matcher(message)
            .replaceAll(match -> Matcher.quoteReplacement(match.group(1) + "***@" + match.group(2)));
        return IBAN.matcher(masked)
            .replaceAll(match -> Matcher.quoteReplacement(match.group(1) + "****" + match.group(2)));
    }

}
