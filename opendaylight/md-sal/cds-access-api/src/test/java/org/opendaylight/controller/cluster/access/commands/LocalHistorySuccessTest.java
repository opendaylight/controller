/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class LocalHistorySuccessTest extends AbstractRequestSuccessTest<LocalHistorySuccess> {

    private static final LocalHistorySuccess OBJECT = new LocalHistorySuccess(
            HISTORY_IDENTIFIER, 0);

    @Test
    public void externalizableProxyTest() throws Exception {
        final LocalHistorySuccessProxyV1 proxy =
                (LocalHistorySuccessProxyV1) OBJECT.externalizableProxy(ABIVersion.BORON);
        Assert.assertNotNull(proxy);
    }

    @Test
    public void cloneAsVersionTest() throws Exception {
        final LocalHistorySuccess clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertEquals(clone.getSequence(), OBJECT.getSequence());
        Assert.assertEquals(clone.getTarget(), OBJECT.getTarget());
        Assert.assertEquals(clone.getVersion(), OBJECT.getVersion());
    }

}
