package org.opendaylight.controller.cluster.access.commands;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class TransactionFailureTest extends AbstractRequestFailureTest<TransactionFailure> {
    private static final TransactionFailure OBJECT = new TransactionFailure(TRANSACTION_IDENTIFIER, 0, CAUSE);

    @Override
    TransactionFailure object() {
        return OBJECT;
    }

    @Test
    public void cloneAsVersion() throws Exception {
        final TransactionFailure clone = OBJECT.cloneAsVersion(ABIVersion.current());
        Assert.assertEquals(OBJECT, clone);
    }

    @Test
    public void externalizableProxy() throws Exception {
        final TransactionFailureProxyV1 proxy = OBJECT.externalizableProxy(ABIVersion.current());
        Assert.assertNotNull(proxy);
    }
}