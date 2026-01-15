package com.example.arithmeticcalculatorapibackend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Percent Controller", description = "Endpoints for percentage calculations")
public class PercentController {

    private static final MathContext DEFAULT_MATH_CONTEXT = MathContext.DECIMAL64;

    // PUBLIC_INTERFACE
    @GetMapping("/percent")
    @Operation(
            summary = "Compute a percent of a number",
            description = "Computes result = (a / 100) * b. Accepts decimal numbers via query parameters a and b."
    )
    public ResponseEntity<?> percent(
            @Parameter(description = "Percentage value (e.g., 10.5 for 10.5%)", required = true, example = "10.5")
            @RequestParam(name = "a", required = false) String aRaw,
            @Parameter(description = "Base number to apply the percentage to", required = true, example = "200")
            @RequestParam(name = "b", required = false) String bRaw
    ) {
        // Validation rules:
        // - Missing a or b => 400 { "error": "Invalid input" }
        // - Non-numeric a or b => 400 { "error": "Invalid input" }
        if (aRaw == null || bRaw == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }

        final BigDecimal a;
        final BigDecimal b;
        try {
            a = new BigDecimal(aRaw.trim());
            b = new BigDecimal(bRaw.trim());
        } catch (RuntimeException ex) {
            // BigDecimal throws NumberFormatException for invalid strings; trim() may throw NPE but inputs are non-null.
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }

        // Compute (a / 100) * b with a reasonable MathContext to avoid non-terminating decimal expansions.
        BigDecimal result = a.divide(BigDecimal.valueOf(100), DEFAULT_MATH_CONTEXT).multiply(b, DEFAULT_MATH_CONTEXT);

        return ResponseEntity.ok(Map.of("result", result));
    }

    // PUBLIC_INTERFACE
    @GetMapping("/modulo")
    @Operation(
            summary = "Compute a modulo b",
            description = "Computes result = a % b. Accepts decimal numbers via query parameters a and b. "
                    + "Returns 400 if inputs are missing/invalid or if b = 0."
    )
    public ResponseEntity<?> modulo(
            @Parameter(description = "Dividend", required = true, example = "10")
            @RequestParam(name = "a", required = false) String aRaw,
            @Parameter(description = "Divisor (must be non-zero)", required = true, example = "3")
            @RequestParam(name = "b", required = false) String bRaw
    ) {
        // Validation rules:
        // - Missing a or b => 400 { "error": "Invalid input" }
        // - Non-numeric a or b => 400 { "error": "Invalid input" }
        // - b == 0 => 400 { "error": "Division by zero" }
        if (aRaw == null || bRaw == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }

        final BigDecimal a;
        final BigDecimal b;
        try {
            a = new BigDecimal(aRaw.trim());
            b = new BigDecimal(bRaw.trim());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }

        if (b.compareTo(BigDecimal.ZERO) == 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Division by zero"));
        }

        // BigDecimal#remainder gives the IEEE 754-style remainder for decimals.
        // Example: 10 % 3 = 1, and it supports decimal inputs (e.g., 10.5 % 3 = 1.5).
        BigDecimal result = a.remainder(b, DEFAULT_MATH_CONTEXT);

        return ResponseEntity.ok(Map.of("result", result));
    }

    // PUBLIC_INTERFACE
    @GetMapping("/power")
    @Operation(
            summary = "Compute a raised to the power of b",
            description = "Computes result = a^b. Accepts decimal numbers via query parameters a and b. "
                    + "Validation/edge-case rules: missing/invalid params => 400 {\"error\":\"Invalid input\"}. "
                    + "0^0 is defined as 1 for this API. Negative bases with non-integer exponents are not supported "
                    + "in the real-number domain and return 400 {\"error\":\"Invalid domain\"}."
    )
    public ResponseEntity<?> power(
            @Parameter(description = "Base (a)", required = true, example = "2")
            @RequestParam(name = "a", required = false) String aRaw,
            @Parameter(description = "Exponent (b)", required = true, example = "3")
            @RequestParam(name = "b", required = false) String bRaw
    ) {
        // Validation rules:
        // - Missing a or b => 400 { "error": "Invalid input" }
        // - Non-numeric a or b => 400 { "error": "Invalid input" }
        // - 0^0 => return 1 (explicitly defined for this API)
        // - Negative base with a non-integer exponent => 400 { "error": "Invalid domain" }
        if (aRaw == null || bRaw == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }

        final BigDecimal a;
        final BigDecimal b;
        try {
            a = new BigDecimal(aRaw.trim());
            b = new BigDecimal(bRaw.trim());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }

        // Define 0^0 = 1 for this API.
        if (a.compareTo(BigDecimal.ZERO) == 0 && b.compareTo(BigDecimal.ZERO) == 0) {
            return ResponseEntity.ok(Map.of("result", BigDecimal.ONE));
        }

        // Negative base with non-integer exponent is not a real number (would be complex).
        if (a.signum() < 0 && !isIntegerValue(b)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid domain"));
        }

        // We use Math.pow over double for broad decimal exponent support in the real domain.
        // This may introduce floating point rounding; we convert back to BigDecimal using valueOf
        // to avoid surprises of new BigDecimal(double).
        double base = a.doubleValue();
        double exponent = b.doubleValue();
        double pow = Math.pow(base, exponent);

        // Guard against overflow/NaN results.
        if (Double.isNaN(pow) || Double.isInfinite(pow)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid domain"));
        }

        return ResponseEntity.ok(Map.of("result", BigDecimal.valueOf(pow)));
    }

    /**
     * Determines if a BigDecimal has no fractional part (e.g., 3.0, 3, -2).
     *
     * @param value BigDecimal to inspect
     * @return true if the value is an integer, else false
     */
    private static boolean isIntegerValue(BigDecimal value) {
        // stripTrailingZeros turns 3.0 into 3, 3.500 into 3.5; scale <= 0 means integer.
        return value.stripTrailingZeros().scale() <= 0;
    }
}
