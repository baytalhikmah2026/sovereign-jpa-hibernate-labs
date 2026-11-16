package com.baytalhikmah.sovereign.service;

/** No fresh price and no recent enough last known price: the honest answer is "try again later". */
public class CatalogueUnavailableException extends RuntimeException {

    public CatalogueUnavailableException(String sku, Throwable cause) {
        super("No price available for " + sku + " right now", cause);
    }

}
