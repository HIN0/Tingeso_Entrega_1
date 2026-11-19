package com.ProyectoTGS.BackCore.web;

import app.GlobalExceptionHandler;
import app.config.SecurityConfig;
import app.utils.SecurityUtils;
import controllers.ClientController;
import controllers.KardexController;
import controllers.LoanController;
import controllers.ReportController;
import controllers.TariffController;
import controllers.ToolController;
import controllers.UserController;
import entities.ToolEntity;
import entities.UserEntity;
import entities.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import services.ClientService;
import services.KardexService;
import services.LoanService;
import services.ReportService;
import services.TariffService;
import services.ToolService;
import services.UserService;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests: real controllers, SecurityConfig and GlobalExceptionHandler; services are mocked.
 * They cover what the service unit tests cannot see: authorization by role, request validation,
 * HTTP error codes and JSON serialization.
 */
@WebMvcTest
@ContextConfiguration(classes = {
        SecurityConfig.class, GlobalExceptionHandler.class,
        ToolController.class, LoanController.class, ClientController.class, TariffController.class,
        KardexController.class, ReportController.class, UserController.class})
class WebLayerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean private ToolService toolService;
    @MockitoBean private LoanService loanService;
    @MockitoBean private ClientService clientService;
    @MockitoBean private TariffService tariffService;
    @MockitoBean private KardexService kardexService;
    @MockitoBean private ReportService reportService;
    @MockitoBean private UserService userService;
    @MockitoBean private SecurityUtils securityUtils;

    // Simulated Keycloak users (the realm role becomes the ROLE_ authority)
    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.claim("preferred_username", "diego")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor employee() {
        return jwt().jwt(j -> j.claim("preferred_username", "juan")).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }

    // =======================================================================
    // AUTHENTICATION AND AUTHORIZATION (Epic 7)
    // =======================================================================

    @Test
    void requestWithoutToken_IsUnauthorized() throws Exception {
        mvc.perform(get("/tools")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpoint_IsPublic() throws Exception {
        // permitted without a token (not 401); the actuator is not part of this slice, so it answers 404
        mvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
    }

    @Test
    void unknownUrl_Returns404NotA500() throws Exception {
        // Before the fix, NoResourceFoundException fell into the generic handler and returned 500
        mvc.perform(get("/tools/does/not/exist").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void wrongHttpMethod_Returns405NotA500() throws Exception {
        mvc.perform(delete("/tariffs").with(admin()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void employee_CanReadToolsAndClients() throws Exception {
        when(toolService.getAllTools()).thenReturn(List.of());
        when(clientService.getAllClients()).thenReturn(List.of());

        mvc.perform(get("/tools").with(employee())).andExpect(status().isOk());
        // class-level @PreAuthorize is ADMIN, but the method-level rule allows EMPLOYEE to read
        mvc.perform(get("/clients").with(employee())).andExpect(status().isOk());
    }

    @Test
    void employee_CannotCreateTools_Gets403WithJsonBody() throws Exception {
        mvc.perform(post("/tools").with(employee())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Saw\",\"category\":\"Carpentry\",\"stock\":1,\"replacementValue\":5000}"))
                .andExpect(status().isForbidden());
        verify(toolService, never()).createTool(any(), any());
    }

    @Test
    void employee_CannotCreateClients_RejectedByUrlRules() throws Exception {
        // Denied by the URL rules of SecurityConfig, before reaching the controller
        mvc.perform(post("/clients").with(employee())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"rut\":\"12.345.678-5\",\"phone\":\"912345678\",\"email\":\"ana@mail.com\"}"))
                .andExpect(status().isForbidden());
        verify(clientService, never()).createClient(any());
    }

    @Test
    void employee_CannotMarkLoanAsPaid_MethodSecurityGives403NotA500() throws Exception {
        // URL rules let EMPLOYEE reach /loans/**, but @PreAuthorize("hasAnyRole('ADMIN')") denies /pay.
        // That AccessDeniedException is thrown inside the controller call; before the fix the
        // generic exception handler turned it into a 500.
        mvc.perform(patch("/loans/1/pay").with(employee()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verify(loanService, never()).markLoanAsPaid(any());
    }

    @Test
    void employee_CannotAccessAdminOnlyAreas() throws Exception {
        mvc.perform(get("/tariffs").with(employee())).andExpect(status().isForbidden());
        mvc.perform(get("/kardex/tool/1").with(employee())).andExpect(status().isForbidden());
        mvc.perform(get("/users/diego").with(employee())).andExpect(status().isForbidden());
    }

    @Test
    void employee_CanUseLoansAndReports() throws Exception {
        when(loanService.getOpenLoans()).thenReturn(List.of());
        when(reportService.getClientsWithLateLoans(isNull(), isNull())).thenReturn(List.of());

        mvc.perform(get("/loans").with(employee())).andExpect(status().isOk());
        mvc.perform(get("/reports/clients/late").with(employee())).andExpect(status().isOk());
    }

    @Test
    void admin_CanAccessTariffs() throws Exception {
        when(tariffService.getTariff()).thenReturn(entities.TariffEntity.builder().dailyRentFee(1000).build());

        mvc.perform(get("/tariffs").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyRentFee").value(1000));
    }

    // =======================================================================
    // REQUEST VALIDATION AND ERROR FORMAT
    // =======================================================================

    @Test
    void createClient_InvalidBody_Returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/clients").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"rut\":\"12.345.678-5\",\"phone\":\"12\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        verify(clientService, never()).createClient(any());
    }

    @Test
    void updateTariff_NegativeFee_Returns400() throws Exception {
        mvc.perform(put("/tariffs").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyRentFee\":1000,\"dailyLateFee\":-5,\"repairFee\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dailyLateFee").exists());
    }

    @Test
    void createTool_IgnoresFieldsTheClientMustNotSet() throws Exception {
        when(toolService.createTool(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/tools").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Saw\",\"category\":\"Carpentry\",\"stock\":3,\"replacementValue\":5000,"
                                + "\"inRepair\":50,\"status\":\"DECOMMISSIONED\",\"id\":99}"))
                .andExpect(status().isOk());

        // The DTO only maps the allowed fields; counters start at 0 (mass assignment protection)
        verify(toolService).createTool(argThat((ToolEntity t) -> t.getId() == null && t.getStatus() == null
                && t.getInRepair() == 0 && t.getLoaned() == 0 && t.getStock() == 3), any());
    }

    @Test
    void businessRuleViolation_Returns400WithMessage() throws Exception {
        when(reportService.getLoansByStatus(eq("UNKNOWN"), isNull(), isNull()))
                .thenThrow(new IllegalArgumentException("No enum constant entities.enums.LoanStatus.UNKNOWN"));

        mvc.perform(get("/reports/loans").param("status", "unknown").with(employee()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No enum constant entities.enums.LoanStatus.UNKNOWN"));
    }

    @Test
    void invalidDateParameter_Returns400() throws Exception {
        mvc.perform(get("/reports/tools/top").param("from", "not-a-date").param("to", "2025-01-31").with(employee()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or missing request parameter: from"));
    }

    @Test
    void malformedJson_Returns400() throws Exception {
        mvc.perform(post("/loans").with(employee())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\": 1, \"toolId\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unexpectedError_Returns500WithoutInternalDetails() throws Exception {
        when(toolService.getAllTools()).thenThrow(new RuntimeException("SQL connection string with secrets"));

        mvc.perform(get("/tools").with(admin()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."));
    }

    // =======================================================================
    // SERIALIZATION
    // =======================================================================

    @Test
    void userPassword_IsNeverSerialized() throws Exception {
        UserEntity user = UserEntity.builder().id(1L).username("diego").password("secret").role(UserRole.ADMIN).build();
        when(userService.findByUsername("diego")).thenReturn(Optional.of(user));

        mvc.perform(get("/users/diego").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("diego"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
