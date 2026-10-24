/**
 * Database access interface for sales orders. Spring Data creates its implementation automatically.
 * JpaRepository<SalesOrder, String> means the entity is SalesOrder and its primary key is a String.
 */
package com.lankaswisstek.salesorder.features.salesorder.repository;

import com.lankaswisstek.salesorder.features.salesorder.model.SalesOrder;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, String> {
    // Spring derives the query from this name: return every order sorted by createdAt descending.
    // createdAt is currently stored as a String, so the database sorts its text value.
    List<SalesOrder> findAllByOrderByCreatedAtDesc();
}
