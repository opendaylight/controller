package org.opendaylight.controller.cluster.access.commands;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class ConnectClientFailureTest extends AbstractRequestFailureTest<ConnectClientFailure> {
    private static final ConnectClientFailure OBJECT = new ConnectClientFailure(CLIENT_IDENTIFIER, 0, CAUSE);

    @Override
    ConnectClientFailure object() {
        return OBJECT;
    }

    @Test
    public void cloneAsVersion() throws Exception {
        final ConnectClientFailure clone = OBJECT.cloneAsVersion(ABIVersion.current());
        Assert.assertEquals(OBJECT.getTarget(), clone.getTarget());
        Assert.assertEquals(OBJECT.getSequence(), clone.getSequence());
        Assert.assertEquals(OBJECT.getCause(), clone.getCause());
    }

    @Test
    public void externalizableProxy() throws Exception {
        final ConnectClientFailureProxyV1 proxy = (ConnectClientFailureProxyV1) OBJECT.externalizableProxy(
                ABIVersion.current());
        Assert.assertNotNull(proxy);
    }
}