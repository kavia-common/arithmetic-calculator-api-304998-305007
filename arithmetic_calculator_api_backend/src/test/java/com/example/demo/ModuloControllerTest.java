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
class ModuloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void modulo_success_returnsResult() throws Exception {
        // 10 % 3 = 1
        mockMvc.perform(get("/modulo").param("a", "10").param("b", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(1));
    }

    @Test
    void modulo_missingParam_returns400WithError() throws Exception {
        mockMvc.perform(get("/modulo").param("a", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid input"));
    }

    @Test
    void modulo_divisionByZero_returns400WithHelpfulError() throws Exception {
        mockMvc.perform(get("/modulo").param("a", "10").param("b", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Division by zero"));
    }
}
