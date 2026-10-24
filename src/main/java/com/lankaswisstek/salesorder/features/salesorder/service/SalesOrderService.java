/** Sales order rules. Quote numbers are canonical, immutable primary keys. */
package com.lankaswisstek.salesorder.features.salesorder.service;

import com.lankaswisstek.salesorder.features.salesorder.model.SalesOrder;
import com.lankaswisstek.salesorder.features.salesorder.repository.SalesOrderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class SalesOrderService {
    private final SalesOrderRepository repository;

    public SalesOrderService(SalesOrderRepository repository) { this.repository = repository; }

    public List<SalesOrder> list() { return repository.findAllByOrderByCreatedAtDesc(); }

    public SalesOrder get(String quoteNumber) {
        return repository.findById(normalize(quoteNumber)).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Sales order not found"));
    }

    public SalesOrder create(SalesOrder order) {
        order.quoteNumber = normalize(order.quoteNumber);
        if (repository.existsById(order.quoteNumber)) throw duplicateQuote();
        // saveAndFlush executes the INSERT here, so database uniqueness failures become HTTP 409.
        return save(order);
    }

    public SalesOrder update(String quoteNumber, SalesOrder order) {
        String key = normalize(quoteNumber);
        SalesOrder existing = get(key);
        if (!key.equals(normalize(order.quoteNumber))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Quote number cannot be changed after the order is created");
        }
        order.quoteNumber = key;
        order.createdAt = existing.createdAt;
        order.markPersisted();
        return save(order);
    }

    public void delete(String quoteNumber) {
        String key = normalize(quoteNumber);
        if (!repository.existsById(key)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sales order not found");
        }
        repository.deleteById(key);
    }

    private SalesOrder save(SalesOrder order) {
        try { return repository.saveAndFlush(order); }
        catch (DataIntegrityViolationException error) {
            // A pre-check alone cannot prevent two users creating the same quote simultaneously.
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                    throw duplicateQuote();
                }
            }
            throw error;
        }
    }

    private ResponseStatusException duplicateQuote() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Quote number already exists");
    }

    private String normalize(String quoteNumber) {
        if (quoteNumber == null || quoteNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quote number is required");
        }
        return quoteNumber.trim().toUpperCase(Locale.ROOT);
    }
}
