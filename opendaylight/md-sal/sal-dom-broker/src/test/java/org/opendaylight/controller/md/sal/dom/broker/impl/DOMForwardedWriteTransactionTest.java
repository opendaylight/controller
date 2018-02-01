/*
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.Collections;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

public class DOMForwardedWriteTransactionTest {


    @Test
    @SuppressWarnings("checkstyle:illegalcatch")
    public void cancelAfterReadyException() {
        DOMStoreWriteTransaction domStoreWriteTransaction = Mockito.mock(DOMStoreWriteTransaction.class);
        AbstractDOMForwardedTransactionFactory abstractDOMForwardedTransactionFactory;
        abstractDOMForwardedTransactionFactory = Mockito.mock(AbstractDOMForwardedTransactionFactory.class);
        doThrow(new RuntimeException()).when(domStoreWriteTransaction).ready();
        DOMForwardedWriteTransaction domForwardedWriteTransaction;
        domForwardedWriteTransaction = new DOMForwardedWriteTransaction<>(
                new Object(),
                Collections.singletonMap(LogicalDatastoreType.OPERATIONAL, domStoreWriteTransaction),
                abstractDOMForwardedTransactionFactory);
        try {
            domForwardedWriteTransaction.submit();
        } catch (RuntimeException t) {
            // nop
        }
        domForwardedWriteTransaction.cancel();
    }

    @Test
    @SuppressWarnings("checkstyle:illegalcatch")
    public void cancelAfterSubmitException() {
        DOMStoreWriteTransaction domStoreWriteTransaction = Mockito.mock(DOMStoreWriteTransaction.class);
        AbstractDOMForwardedTransactionFactory abstractDOMForwardedTransactionFactory;
        abstractDOMForwardedTransactionFactory = Mockito.mock(AbstractDOMForwardedTransactionFactory.class);
        doThrow(new RuntimeException()).when(abstractDOMForwardedTransactionFactory).submit(any(), any());
        DOMForwardedWriteTransaction domForwardedWriteTransaction;
        domForwardedWriteTransaction = new DOMForwardedWriteTransaction<>(
                new Object(),
                Collections.singletonMap(LogicalDatastoreType.OPERATIONAL, domStoreWriteTransaction),
                abstractDOMForwardedTransactionFactory);
        try {
            domForwardedWriteTransaction.submit();
        } catch (RuntimeException t) {
            // nop
        }
        domForwardedWriteTransaction.cancel();
    }
}