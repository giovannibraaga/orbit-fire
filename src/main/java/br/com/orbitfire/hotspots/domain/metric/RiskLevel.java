package br.com.orbitfire.hotspots.domain.metric;

/**
 * INPE fire-risk classes for {@code risco_fogo} (0–1). Thresholds are kept as
 * constants so they can be recalibrated against real data.
 *
 * <pre>
 *   MINIMO  [0.00, 0.15)
 *   BAIXO   [0.15, 0.40)
 *   MEDIO   [0.40, 0.70)
 *   ALTO    [0.70, 0.95)
 *   CRITICO [0.95, 1.00]
 * </pre>
 *
 * "Very high risk" is defined as risk &gt;= {@link #VERY_HIGH_THRESHOLD} (ALTO + CRITICO).
 */
public enum RiskLevel {

    MINIMO,
    BAIXO,
    MEDIO,
    ALTO,
    CRITICO;

    public static final double BAIXO_MIN = 0.15;
    public static final double MEDIO_MIN = 0.40;
    public static final double ALTO_MIN = 0.70;
    public static final double CRITICO_MIN = 0.95;

    public static final double VERY_HIGH_THRESHOLD = ALTO_MIN;

    /**
     * @return the class for {@code risk}, or {@code null} when {@code risk} is null
     */
    public static RiskLevel classify(Double risk) {
        if (risk == null) {
            return null;
        }
        if (risk < BAIXO_MIN) {
            return MINIMO;
        }
        if (risk < MEDIO_MIN) {
            return BAIXO;
        }
        if (risk < ALTO_MIN) {
            return MEDIO;
        }
        if (risk < CRITICO_MIN) {
            return ALTO;
        }
        return CRITICO;
    }

    public static boolean isVeryHigh(Double risk) {
        return risk != null && risk >= VERY_HIGH_THRESHOLD;
    }
}
