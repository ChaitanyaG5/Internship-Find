package com.example.internshiptracker.source;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

final class DescriptionSanitizer {

    private static final int MAX_LENGTH = 10_000;

    private DescriptionSanitizer() {
    }

    static String plainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String text = Jsoup.clean(html, Safelist.none()).trim();
        return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
    }
}
