package com.baytalhikmah.sovereign.persistence;

/** A row that must exist does not: names the entity and the ID that was asked for. */
public class RecordNotFoundException extends RuntimeException {

    public RecordNotFoundException(String entityName, Object id) {
        super(entityName + " " + id + " not found");
    }

}
