package com.example.arithmeticcalculatorapibackend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PowerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void power_success_returnsResult() throws Exception {
        // 2^3 = 8
        mockMvc.perform(get("/power").param("a", "2").param("b", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(8));
    }

    @Test
    void power_missingParam_returns400WithError() throws Exception {
        mockMvc.perform(get("/power").param("a", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid input"));
    }

    @Test
    void power_negativeBaseWithFractionalExponent_returns400WithError() throws Exception {
        // (-2)^(0.5) is not a real number (complex), so we return 400.
        mockMvc.perform(get("/power").param("a", "-2").param("b", "0.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid domain"));
    }
}
