/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.getWithTimeout;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ClientSnapshotTest extends AbstractClientHandleTest<ClientSnapshot> {
    private static final YangInstanceIdentifier PATH = YangInstanceIdentifier.of();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        doReturn(Optional.empty()).when(getDataTreeSnapshot()).readNode(PATH);
    }

    @Override
    protected ClientSnapshot createHandle(final AbstractClientHistory parent) {
        return parent.takeSnapshot();
    }

    @Override
    protected void doHandleOperation(final ClientSnapshot snapshot) {
        snapshot.read(PATH);
    }

    @Test
    public void testExists() throws Exception {
        final var exists = getHandle().exists(PATH);
        verify(getDataTreeSnapshot()).readNode(PATH);
        assertEquals(Boolean.FALSE, getWithTimeout(exists));
    }

    @Test
    public void testRead() throws Exception {
        final var read = getHandle().read(PATH);
        verify(getDataTreeSnapshot()).readNode(PATH);
        assertFalse(getWithTimeout(read).isPresent());
    }
}
