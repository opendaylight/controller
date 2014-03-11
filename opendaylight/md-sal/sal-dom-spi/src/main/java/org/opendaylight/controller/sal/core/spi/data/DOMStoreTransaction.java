package org.opendaylight.controller.sal.core.spi.data;

import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * DOM Data Store transaction
 *
 * See {@link DOMStoreReadTransaction}, {@link DOMStoreWriteTransaction} and {@link DOMStoreReadWriteTransaction}
 * for specific transaction types.
 *
 */
public interface DOMStoreTransaction extends AutoCloseable, Identifiable<Object> {


    /**
     *
     * Unique identifier of the transaction
     *
     */
    @Override
    public Object getIdentifier();
}
