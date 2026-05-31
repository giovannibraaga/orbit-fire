package br.com.orbitfire.hotspots.domain.model;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import br.com.orbitfire.hotspots.shared.text.TextNormalizer;

/**
 * Resolves the two-letter UF code from a Brazilian state's full name.
 *
 * <p>The INPE CSV carries {@code estado} (full name, e.g. "MATO GROSSO") but no
 * UF abbreviation. Lookup is accent- and case-insensitive so it tolerates
 * spelling variations between files. Backed by {@link BrazilianState}.
 */
public final class BrazilianStateResolver {

    private static final Map<String, String> NAME_TO_UF = Arrays.stream(BrazilianState.values())
            .collect(Collectors.toMap(
                    s -> TextNormalizer.normalize(s.displayName()),
                    BrazilianState::uf));

    private BrazilianStateResolver() {
    }

    /**
     * @return the UF code, or {@code null} if {@code stateName} is blank or
     *         unrecognized
     */
    public static String toUf(String stateName) {
        if (stateName == null || stateName.isBlank()) {
            return null;
        }
        return NAME_TO_UF.get(TextNormalizer.normalize(stateName));
    }
}
