package br.com.orbitfire.hotspots.domain.model;

/**
 * Helpers for the INPE {@code data_hora_gmt} field, which the parser keeps as a
 * raw string (formats like {@code "2026/05/31 13:45:00"} or
 * {@code "2026-05-31T13:45:00"}).
 */
public final class HotspotTime {

    private HotspotTime() {
    }

    /**
     * @return the hour-of-day (0–23), or {@code null} when not parseable
     */
    public static Integer extractHour(String dateTimeGmt) {
        if (dateTimeGmt == null) {
            return null;
        }
        int sep = dateTimeGmt.indexOf(' ');
        if (sep < 0) {
            sep = dateTimeGmt.indexOf('T');
        }
        if (sep < 0 || sep + 1 >= dateTimeGmt.length()) {
            return null;
        }
        String time = dateTimeGmt.substring(sep + 1);
        int colon = time.indexOf(':');
        String hh = colon < 0 ? time : time.substring(0, colon);
        try {
            int hour = Integer.parseInt(hh.trim());
            return (hour >= 0 && hour <= 23) ? hour : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
