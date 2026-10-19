package dev.edney.lingolocal;

import java.text.Normalizer;
import java.util.Locale;

/** Normalização compartilhada; fala compara transcrição, sem simular nota fonética. */
final class ExerciseRules {
    static String normalize(String value) {
        return Normalizer.normalize(value,Normalizer.Form.NFC).toLowerCase(Locale.ROOT)
            .replaceAll("[\\p{Punct}’]", "").replaceAll("\\s+", " ").trim();
    }
    static boolean matches(String actual,String expected) { return normalize(actual).equals(normalize(expected)); }
}
