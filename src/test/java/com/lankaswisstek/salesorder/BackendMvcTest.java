/** HTTP contract tests with real controllers/services and mocked repositories; no database is opened. */
package com.lankaswisstek.salesorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lankaswisstek.salesorder.features.salesorder.controller.SalesOrderController;
import com.lankaswisstek.salesorder.features.salesorder.model.SalesOrder;
import com.lankaswisstek.salesorder.features.salesorder.repository.SalesOrderRepository;
import com.lankaswisstek.salesorder.features.salesorder.service.SalesOrderService;
import com.lankaswisstek.salesorder.features.settings.controller.SettingsController;
import com.lankaswisstek.salesorder.features.settings.model.WorkspaceSettings;
import com.lankaswisstek.salesorder.features.settings.repository.WorkspaceSettingsRepository;
import com.lankaswisstek.salesorder.features.settings.service.SettingsService;
import com.lankaswisstek.salesorder.shared.config.WebConfig;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({SalesOrderController.class, SettingsController.class})
@Import({SalesOrderService.class, SettingsService.class, WebConfig.class})
class BackendMvcTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean SalesOrderRepository orders;
    @MockitoBean WorkspaceSettingsRepository settings;

    private SalesOrder order() {
        SalesOrder order = new SalesOrder();
        order.customerName = "Test customer";
        order.quoteNumber = "Q-1";
        order.salesPerson = "Ashish";
        order.punchInDate = "2026-10-20";
        order.createdAt = "2026-10-20T10:00:00Z";
        order.quotationName = "quote.pdf";
        order.quotationData = "data:application/pdf;base64,dGVzdA==";
        order.aluminiumImageData = "data:image/png;base64,dGVzdA==";
        order.meshImageData = "data:image/png;base64,bWVzaA==";
        return order;
    }

    @Test void listAndGetKeepExistingJson() throws Exception {
        SalesOrder order = order();
        when(orders.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(orders.findById(order.quoteNumber)).thenReturn(Optional.of(order));
        mvc.perform(get("/api/orders")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").value(order.customerName));
        mvc.perform(get("/api/orders/{id}", order.quoteNumber)).andExpect(status().isOk())
                .andExpect(jsonPath("$.quotationData").value(order.quotationData));
    }

    @Test void createDelegatesToServiceAndReturns201() throws Exception {
        when(orders.saveAndFlush(any(SalesOrder.class))).thenAnswer(call -> call.getArgument(0));
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.quoteNumber").value("Q-1"));
        verify(orders).saveAndFlush(any(SalesOrder.class));
    }

    @Test void duplicateQuoteReturns409WithoutSaving() throws Exception {
        when(orders.existsById("Q-1")).thenReturn(true);
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order())))
                .andExpect(status().isConflict());
        verify(orders, never()).saveAndFlush(any(SalesOrder.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"customerName", "quoteNumber", "salesPerson", "punchInDate", "createdAt"})
    void invalidRequiredFieldReturns400(String field) throws Exception {
        SalesOrder invalid = order();
        SalesOrder.class.getField(field).set(invalid, " ");
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orders);
    }

    @Test void updateRetainsQuoteKeyCreationTimeAndAttachments() throws Exception {
        SalesOrder existing = order();
        when(orders.findById("Q-1")).thenReturn(Optional.of(existing));
        when(orders.saveAndFlush(any(SalesOrder.class))).thenAnswer(call -> call.getArgument(0));
        SalesOrder changed = order();
        changed.createdAt = "2099-01-01T00:00:00Z";
        mvc.perform(put("/api/orders/Q-1").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(changed)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quoteNumber").value("Q-1"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.createdAt").value(existing.createdAt))
                .andExpect(jsonPath("$.quotationData").value(existing.quotationData))
                .andExpect(jsonPath("$.aluminiumImageData").value(existing.aluminiumImageData))
                .andExpect(jsonPath("$.meshImageData").value(existing.meshImageData));
        verify(orders).saveAndFlush(argThat(saved -> saved.quoteNumber.equals("Q-1") && !saved.isNew()));
    }

    @Test void primaryKeyCannotBeRenamed() throws Exception {
        when(orders.findById("Q-1")).thenReturn(Optional.of(order()));
        SalesOrder changed = order();
        changed.quoteNumber = "Q-2";
        mvc.perform(put("/api/orders/Q-1").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(changed)))
                .andExpect(status().isBadRequest());
        verify(orders, never()).saveAndFlush(any(SalesOrder.class));
    }

    @Test void createNormalizesQuoteAndUsesInsertSemantics() throws Exception {
        when(orders.saveAndFlush(any(SalesOrder.class))).thenAnswer(call -> call.getArgument(0));
        SalesOrder order = order();
        order.quoteNumber = " q-1 ";
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.quoteNumber").value("Q-1"))
                .andExpect(jsonPath("$.id").doesNotExist());
        verify(orders).saveAndFlush(argThat(saved -> saved.quoteNumber.equals("Q-1") && saved.isNew()));
    }

    @Test void caseAndWhitespaceVariantsAreDuplicateQuotes() throws Exception {
        when(orders.existsById("Q-1")).thenReturn(true);
        SalesOrder order = order();
        order.quoteNumber = " q-1 ";
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order)))
                .andExpect(status().isConflict());
        verify(orders, never()).saveAndFlush(any(SalesOrder.class));
    }

    @Test void concurrentDuplicateInsertReturns409() throws Exception {
        when(orders.saveAndFlush(any(SalesOrder.class))).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException("duplicate",
                        new java.sql.SQLException("duplicate primary key", "23505")));
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order())))
                .andExpect(status().isConflict());
    }

    @Test void queryRoutesHandleQuotesContainingSlashes() throws Exception {
        SalesOrder order = order();
        order.quoteNumber = "Q/2026/1";
        when(orders.findById(order.quoteNumber)).thenReturn(Optional.of(order));
        when(orders.existsById(order.quoteNumber)).thenReturn(true);
        when(orders.saveAndFlush(any(SalesOrder.class))).thenAnswer(call -> call.getArgument(0));
        mvc.perform(get("/api/orders").param("quoteNumber", order.quoteNumber))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quoteNumber").value(order.quoteNumber));
        mvc.perform(put("/api/orders").param("quoteNumber", order.quoteNumber)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(order)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/orders").param("quoteNumber", order.quoteNumber))
                .andExpect(status().isNoContent());
        verify(orders).deleteById("Q/2026/1");
    }

    @Test void deleteReturns204AndRemovesOnlyRequestedId() throws Exception {
        when(orders.existsById("Q-1")).thenReturn(true);
        mvc.perform(delete("/api/orders/Q-1")).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(orders).deleteById("Q-1");
    }

    @Test void unknownOrderReturns404ForGetUpdateAndDelete() throws Exception {
        mvc.perform(get("/api/orders/missing")).andExpect(status().isNotFound());
        mvc.perform(put("/api/orders/missing").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(order())))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/orders/missing")).andExpect(status().isNotFound());
        verify(orders, never()).saveAndFlush(any(SalesOrder.class));
        verify(orders, never()).deleteById(any());
    }

    @Test void peopleDefaultsAreStoredOnFirstRead() throws Exception {
        mvc.perform(get("/api/settings/sales-people")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Manjunath"))
                .andExpect(jsonPath("$.length()").value(3));
        verify(settings).save(argThat(row -> row.settingKey.equals("salesPeople")
                && row.settingValue.equals("[\"Manjunath\",\"Ashish\",\"Vijay\"]")));
    }

    @Test void peopleAreTrimmedDeduplicatedAndSaved() throws Exception {
        mvc.perform(put("/api/settings/sales-people").contentType(MediaType.APPLICATION_JSON)
                        .content("[\" Ashish \",\"\",\"Ashish\",\"Vijay\"]"))
                .andExpect(status().isOk()).andExpect(content().json("[\"Ashish\",\"Vijay\"]"));
        verify(settings).save(argThat(row -> row.settingValue.equals("[\"Ashish\",\"Vijay\"]")));
    }

    @Test void savedPeopleAreDecodedAndMalformedJsonKeepsFallback() throws Exception {
        when(settings.findById("salesPeople")).thenReturn(
                Optional.of(new WorkspaceSettings("salesPeople", "[\"Ashish\"]")),
                Optional.of(new WorkspaceSettings("salesPeople", "invalid JSON")));
        mvc.perform(get("/api/settings/sales-people")).andExpect(content().json("[\"Ashish\"]"));
        mvc.perform(get("/api/settings/sales-people")).andExpect(content().json("[]"));
    }

    @Test void workspaceReadAndWriteKeepCompanyNameContract() throws Exception {
        mvc.perform(get("/api/settings/workspace")).andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Lanka Swisstek"));
        when(settings.findById("workspaceName")).thenReturn(
                Optional.of(new WorkspaceSettings("workspaceName", "Saved company")));
        mvc.perform(get("/api/settings/workspace")).andExpect(jsonPath("$.companyName").value("Saved company"));
        mvc.perform(put("/api/settings/workspace").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("companyName", " New company "))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.companyName").value("New company"));
        verify(settings).save(argThat(row -> row.settingKey.equals("workspaceName")
                && row.settingValue.equals("New company")));
    }

    @Test void blankWorkspaceReturns400WithoutSaving() throws Exception {
        mvc.perform(put("/api/settings/workspace").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verify(settings, never()).save(any(WorkspaceSettings.class));
    }

    @Test void corsStillAllowsLocalFrontendAndDelete() throws Exception {
        mvc.perform(options("/api/orders/Q-1").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test void relocatedModelsKeepExistingTableNames() {
        assertEquals("sales_orders", SalesOrder.class.getAnnotation(Table.class).name());
        assertEquals("workspace_settings", WorkspaceSettings.class.getAnnotation(Table.class).name());
    }
}

