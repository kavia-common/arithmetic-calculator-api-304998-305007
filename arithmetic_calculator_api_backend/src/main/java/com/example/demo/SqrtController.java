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
@Tag(name = "Sqrt Controller", description = "Endpoints for square root calculations")
public class SqrtController {

    private static final MathContext DEFAULT_MATH_CONTEXT = MathContext.DECIMAL64;

    // PUBLIC_INTERFACE
    @GetMapping("/sqrt")
    @Operation(
            summary = "Compute the square root of a number",
            description = "Computes result = sqrt(a). Accepts a decimal number via query parameter a. "
                    + "Returns 400 if a is missing/invalid or if a is negative (domain error)."
    )
    public ResponseEntity<?> sqrt(
            @Parameter(description = "Value to compute the square root of (must be >= 0)", required = true, example = "9")
            @RequestParam(name = "a", required = false) String aRaw
    ) {
        // Validation rules:
        // - Missing a => 400 { "error": "Invalid input" }
        // - Non-numeric a => 400 { "error": "Invalid input" }
        // - a < 0 => 400 { "error": "Invalid domain" }
        if (aRaw == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }

        final BigDecimal a;
        try {
            a = new BigDecimal(aRaw.trim());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
        }

        if (a.signum() < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid domain"));
        }

        // For DECIMAL64 precision consistent with other endpoints, compute sqrt using double and convert
        // back via BigDecimal.valueOf to avoid new BigDecimal(double) surprises.
        double value = a.doubleValue();
        double sqrt = Math.sqrt(value);

        // Guard against overflow/NaN results (shouldn't happen for a >= 0 unless extremely large).
        if (Double.isNaN(sqrt) || Double.isInfinite(sqrt)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid domain"));
        }

        // Wrap in BigDecimal for consistent JSON number rendering with other endpoints.
        // Use MathContext rounding to keep precision bounded and predictable.
        BigDecimal result = BigDecimal.valueOf(sqrt).round(DEFAULT_MATH_CONTEXT);

        return ResponseEntity.ok(Map.of("result", result));
    }
}
