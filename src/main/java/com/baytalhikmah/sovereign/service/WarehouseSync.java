package com.baytalhikmah.sovereign.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.baytalhikmah.sovereign.domain.StockChanged;

/** Passes stock changes to the external warehouse system, but only once the transaction has committed. */
@Component
public class WarehouseSync {

    private final WarehouseGateway gateway;

    public WarehouseSync(WarehouseGateway gateway) {
        this.gateway = gateway;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void afterCommit(StockChanged change) {
        gateway.send(change);
    }

}
