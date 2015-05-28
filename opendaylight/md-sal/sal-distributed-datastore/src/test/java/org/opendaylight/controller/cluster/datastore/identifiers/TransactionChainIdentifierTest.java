package org.opendaylight.controller.cluster.datastore.identifiers;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TransactionChainIdentifierTest {
    @Test
    public void testToString(){
        TransactionChainIdentifier transactionChainIdentifier = new TransactionChainIdentifier("member-1", 99);

        String id = transactionChainIdentifier.toString();

        assertEquals("member-1-chn-99", id);
    }

    @Test
    public void testNewTransactionIdentifier(){
        TransactionChainIdentifier transactionChainIdentifier = new TransactionChainIdentifier("member-1", 99);

        TransactionIdentifier txId1 = transactionChainIdentifier.newTransactionIdentifier();

        assertEquals("member-1-chn-99-txn-1", txId1.toString());

        TransactionIdentifier txId2 = transactionChainIdentifier.newTransactionIdentifier();

        assertEquals("member-1-chn-99-txn-2", txId2.toString());
    }

}