package services;

/**
 * Chilean RUT validation (modulo 11 check digit) and normalization.
 * Accepts "12.345.678-5", "12345678-5" or "123456785" and returns the canonical "12.345.678-5".
 */
public final class RutValidator {

    private RutValidator() {
    }

    public static String normalize(String rut) {
        if (rut == null || rut.isBlank()) {
            throw new IllegalArgumentException("RUT is required.");
        }
        String clean = rut.replace(".", "").replace("-", "").replace(" ", "").toUpperCase();
        if (!clean.matches("\\d{7,8}[0-9K]")) {
            throw new IllegalArgumentException("Invalid RUT format: " + rut);
        }
        String body = clean.substring(0, clean.length() - 1);
        char checkDigit = clean.charAt(clean.length() - 1);
        if (computeCheckDigit(body) != checkDigit) {
            throw new IllegalArgumentException("Invalid RUT check digit: " + rut);
        }
        return format(body) + "-" + checkDigit;
    }

    // Modulo 11: multiply digits right-to-left by 2,3,4,5,6,7,2,3... and use 11 - (sum % 11)
    static char computeCheckDigit(String body) {
        int sum = 0;
        int factor = 2;
        for (int i = body.length() - 1; i >= 0; i--) {
            sum += Character.getNumericValue(body.charAt(i)) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int result = 11 - (sum % 11);
        if (result == 11) return '0';
        if (result == 10) return 'K';
        return (char) ('0' + result);
    }

    // 12345678 -> 12.345.678
    private static String format(String body) {
        StringBuilder formatted = new StringBuilder(body);
        for (int i = formatted.length() - 3; i > 0; i -= 3) {
            formatted.insert(i, '.');
        }
        return formatted.toString();
    }
}
