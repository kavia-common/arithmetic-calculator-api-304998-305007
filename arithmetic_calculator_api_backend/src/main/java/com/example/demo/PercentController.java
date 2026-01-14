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
}
