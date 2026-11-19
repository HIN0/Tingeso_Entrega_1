package com.ProyectoTGS.BackCore.web;

import dtos.CreateToolRequest;
import entities.ToolEntity;
import entities.UserEntity;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import repositories.ToolRepository;
import services.KardexService;
import services.ToolService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Runs ToolService behind Spring's method-validation proxy (@Validated + @Valid), which plain
 * unit tests bypass. Regression test for a real bug: CreateToolRequest did not initialise the
 * counters, so the proxy rejected every new tool even though all unit tests were green.
 */
@SpringJUnitConfig(ServiceValidationTest.Config.class)
class ServiceValidationTest {

    @Configuration
    @Import({ToolService.class, ValidationAutoConfiguration.class})
    static class Config {
    }

    @Autowired
    private ToolService toolService;

    @MockBean private ToolRepository toolRepository;
    @MockBean private KardexService kardexService;

    private final UserEntity user = UserEntity.builder().id(1L).username("diego").build();

    @Test
    void toolBuiltFromTheCreateRequest_PassesServiceValidation() {
        when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ToolEntity tool = new CreateToolRequest("Saw", "Carpentry", 2, 5000).toEntity();

        assertDoesNotThrow(() -> toolService.createTool(tool, user));
    }

    @Test
    void validationProxyIsActive_InvalidEntityIsRejected() {
        // Proves the previous test is meaningful: without the proxy this would not throw
        ToolEntity invalid = ToolEntity.builder().name("Saw").category("Carpentry").stock(2)
                .replacementValue(5000).inRepair(null).build();

        assertThrows(ConstraintViolationException.class, () -> toolService.createTool(invalid, user));
    }
}
