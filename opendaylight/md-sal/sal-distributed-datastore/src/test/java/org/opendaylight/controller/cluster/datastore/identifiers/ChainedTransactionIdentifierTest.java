/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.identifiers;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
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

        assertThat(txnId, startsWith("member-1-chn-99-txn-100-"));
    }

}