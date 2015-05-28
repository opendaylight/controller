package org.opendaylight.controller.cluster.datastore.identifiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ChainedTransactionIdentifierTest {

    @Test
    public void testToString(){
        TransactionChainIdentifier chainId = new TransactionChainIdentifier("member-1", 99);
        ChainedTransactionIdentifier chainedTransactionIdentifier = new ChainedTransactionIdentifier(chainId, 100);

        String txnId = chainedTransactionIdentifier.toString();

        assertTrue(txnId.contains("member-1"));
        assertTrue(txnId.contains("100"));
        assertTrue(txnId.contains("99"));

        assertEquals("member-1-chn-99-txn-100", txnId);
    }

}