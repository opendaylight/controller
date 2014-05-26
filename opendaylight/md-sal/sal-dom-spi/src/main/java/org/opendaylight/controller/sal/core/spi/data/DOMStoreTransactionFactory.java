package org.opendaylight.controller.sal.core.spi.data;

public interface DOMStoreTransactionFactory {

    /**
    *
    * Creates a read only transaction
    *
    * @return
    */
   DOMStoreReadTransaction newReadOnlyTransaction();

   /**
    * Creates write only transaction
    *
    * @return
    */
   DOMStoreWriteTransaction newWriteOnlyTransaction();

   /**
    * Creates Read-Write transaction
    *
    * @return
    */
   DOMStoreReadWriteTransaction newReadWriteTransaction();

}
