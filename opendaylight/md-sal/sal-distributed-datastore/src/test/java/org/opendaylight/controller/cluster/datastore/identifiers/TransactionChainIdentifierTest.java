/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.identifiers;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

public class TransactionChainIdentifierTest {
    private static final MemberName MEMBER_1 = MemberName.forName("member-1");

    @Test
    public void testToString(){
        TransactionChainIdentifier transactionChainIdentifier = new TransactionChainIdentifier(MEMBER_1, 99);

        String id = transactionChainIdentifier.toString();

        assertEquals("member-1-chn-99", id);
    }

    @Test
    public void testNewTransactionIdentifier(){
        TransactionChainIdentifier transactionChainIdentifier = new TransactionChainIdentifier(MEMBER_1, 99);

        TransactionIdentifier txId1 = transactionChainIdentifier.newTransactionIdentifier();

        assertThat(txId1.toString(), startsWith("member-1-chn-99-txn-1-"));

        TransactionIdentifier txId2 = transactionChainIdentifier.newTransactionIdentifier();

        assertThat(txId2.toString(), startsWith("member-1-chn-99-txn-2-"));
    }

}