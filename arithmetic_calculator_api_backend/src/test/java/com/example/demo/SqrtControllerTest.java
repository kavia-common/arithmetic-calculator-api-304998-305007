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
class SqrtControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sqrt_success_returnsResult() throws Exception {
        // sqrt(9) = 3
        mockMvc.perform(get("/sqrt").param("a", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(3));
    }

    @Test
    void sqrt_missingParam_returns400WithError() throws Exception {
        mockMvc.perform(get("/sqrt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid input"));
    }

    @Test
    void sqrt_negativeInput_returns400WithDomainError() throws Exception {
        mockMvc.perform(get("/sqrt").param("a", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid domain"));
    }
}
