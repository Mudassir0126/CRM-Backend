/** REST endpoints use quote number as the identifier. Query routes also support quotes with slashes. */
package com.lankaswisstek.salesorder.features.salesorder.controller;

import com.lankaswisstek.salesorder.features.salesorder.model.SalesOrder;
import com.lankaswisstek.salesorder.features.salesorder.service.SalesOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class SalesOrderController {
    private final SalesOrderService service;
    public SalesOrderController(SalesOrderService service) { this.service = service; }

    @GetMapping(params = "!quoteNumber")
    public List<SalesOrder> list() { return service.list(); }

    @GetMapping("/{quoteNumber}")
    public SalesOrder get(@PathVariable String quoteNumber) { return service.get(quoteNumber); }

    @GetMapping(params = "quoteNumber")
    public SalesOrder getByQuote(@RequestParam String quoteNumber) { return service.get(quoteNumber); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalesOrder create(@Valid @RequestBody SalesOrder order) { return service.create(order); }

    @PutMapping("/{quoteNumber}")
    public SalesOrder update(@PathVariable String quoteNumber, @Valid @RequestBody SalesOrder order) {
        return service.update(quoteNumber, order);
    }

    @PutMapping(params = "quoteNumber")
    public SalesOrder updateByQuote(@RequestParam String quoteNumber, @Valid @RequestBody SalesOrder order) {
        return service.update(quoteNumber, order);
    }

    @DeleteMapping("/{quoteNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String quoteNumber) { service.delete(quoteNumber); }

    @DeleteMapping(params = "quoteNumber")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByQuote(@RequestParam String quoteNumber) { service.delete(quoteNumber); }
}
