package com.baytalhikmah.sovereign.persistence;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.baytalhikmah.sovereign.domain.InventoryItem;
import com.baytalhikmah.sovereign.domain.StockChanged;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

/**
 * JPA entity listener of InventoryItem. It runs inside the transaction, right after Hibernate has written
 * the row, so it only announces the change; the external system hears about it after the commit.
 */
@Component
public class StockChangeListener {

    private final ApplicationEventPublisher events;

    public StockChangeListener(ApplicationEventPublisher events) {
        this.events = events;
    }

    @PostPersist
    @PostUpdate
    void stockWritten(InventoryItem item) {
        events.publishEvent(new StockChanged(item.getWarehouseCode(), item.getSku(), item.getQuantityOnHand(),
            item.getVersion()));
    }

}
