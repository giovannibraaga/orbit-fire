package br.com.orbitfire.hotspots.shared.text;

import java.text.Normalizer;

/**
 * Normalizes free text for accent- and case-insensitive comparisons: strips
 * diacritics, collapses whitespace and upper-cases. Used so that static filter
 * options (e.g. "Amazônia") still match raw CSV values (e.g. "Amazonia").
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String stripped = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return stripped.trim().replaceAll("\\s+", " ").toUpperCase();
    }
}
