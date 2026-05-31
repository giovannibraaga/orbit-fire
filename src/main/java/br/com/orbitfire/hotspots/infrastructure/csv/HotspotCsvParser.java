package br.com.orbitfire.hotspots.infrastructure.csv;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.orbitfire.hotspots.domain.model.BrazilianStateResolver;
import br.com.orbitfire.hotspots.domain.model.Hotspot;

/**
 * Parses INPE "focos" CSV files into {@link Hotspot} domain records.
 *
 * <p>Columns are resolved by header name (not position), so the parser tolerates
 * reordered or missing columns. Invalid numeric sentinels ({@code -999}) and
 * empty cells become {@code null}.
 *
 * <p>Expected header:
 * {@code id,lat,lon,data_hora_gmt,satelite,municipio,estado,pais,municipio_id,
 * estado_id,pais_id,numero_dias_sem_chuva,precipitacao,risco_fogo,bioma,frp}
 */
@Component
public class HotspotCsvParser {

    private static final char DELIMITER = ',';

    public List<Hotspot> parse(byte[] csv) {
        String content = new String(csv, StandardCharsets.UTF_8);
        String[] lines = content.split("\\r?\\n");
        if (lines.length == 0 || lines[0].isBlank()) {
            return List.of();
        }

        Map<String, Integer> columns = headerIndex(splitLine(lines[0]));
        List<Hotspot> hotspots = new ArrayList<>(lines.length);
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            hotspots.add(toHotspot(splitLine(lines[i]), columns));
        }
        return hotspots;
    }

    private Hotspot toHotspot(String[] fields, Map<String, Integer> columns) {
        String state = str(fields, columns, "estado");
        return new Hotspot(
                str(fields, columns, "id"),
                dbl(fields, columns, "lat"),
                dbl(fields, columns, "lon"),
                str(fields, columns, "data_hora_gmt"),
                str(fields, columns, "satelite"),
                str(fields, columns, "municipio"),
                state,
                BrazilianStateResolver.toUf(state),
                str(fields, columns, "pais"),
                str(fields, columns, "municipio_id"),
                str(fields, columns, "estado_id"),
                str(fields, columns, "pais_id"),
                integer(fields, columns, "numero_dias_sem_chuva"),
                dbl(fields, columns, "precipitacao"),
                dbl(fields, columns, "risco_fogo"),
                str(fields, columns, "bioma"),
                dbl(fields, columns, "frp"));
    }

    private Map<String, Integer> headerIndex(String[] header) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            index.put(header[i].trim().toLowerCase(), i);
        }
        return index;
    }

    private String str(String[] fields, Map<String, Integer> columns, String name) {
        String raw = raw(fields, columns, name);
        return isNullValue(raw) ? null : raw;
    }

    private Double dbl(String[] fields, Map<String, Integer> columns, String name) {
        String raw = raw(fields, columns, name);
        if (isNullValue(raw)) {
            return null;
        }
        try {
            double value = Double.parseDouble(raw);
            return isSentinel(value) ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer integer(String[] fields, Map<String, Integer> columns, String name) {
        Double value = dbl(fields, columns, name);
        return value == null ? null : value.intValue();
    }

    private String raw(String[] fields, Map<String, Integer> columns, String name) {
        Integer idx = columns.get(name);
        if (idx == null || idx >= fields.length) {
            return null;
        }
        return fields[idx];
    }

    private boolean isNullValue(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.equals("-999");
    }

    private boolean isSentinel(double value) {
        return value == -999.0;
    }

    /**
     * Minimal RFC 4180-style splitter: handles double-quoted fields and escaped
     * quotes ({@code ""}). State/municipality names from INPE are unquoted, but
     * this keeps the parser safe if a quoted field ever appears.
     */
    private String[] splitLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == DELIMITER) {
                out.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        out.add(field.toString());
        return out.toArray(new String[0]);
    }
}
