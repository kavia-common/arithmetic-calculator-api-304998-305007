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
class PercentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void percent_success_returnsResult() throws Exception {
        // (10 / 100) * 200 = 20
        mockMvc.perform(get("/percent").param("a", "10").param("b", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(20));
    }

    @Test
    void percent_missingParam_returns400WithError() throws Exception {
        mockMvc.perform(get("/percent").param("a", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid input"));
    }
}
