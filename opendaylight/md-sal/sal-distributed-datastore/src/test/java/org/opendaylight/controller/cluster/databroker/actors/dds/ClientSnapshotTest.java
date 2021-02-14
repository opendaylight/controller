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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.getWithTimeout;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ClientSnapshotTest extends AbstractClientHandleTest<ClientSnapshot> {

    private static final YangInstanceIdentifier PATH = YangInstanceIdentifier.empty();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(getDataTreeSnapshot().readNode(PATH)).thenReturn(Optional.empty());
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
        final ListenableFuture<Boolean> exists = getHandle().exists(PATH);
        verify(getDataTreeSnapshot()).readNode(PATH);
        assertEquals(Boolean.FALSE, getWithTimeout(exists));
    }

    @Test
    public void testRead() throws Exception {
        final ListenableFuture<Optional<NormalizedNode>> exists = getHandle().read(PATH);
        verify(getDataTreeSnapshot()).readNode(PATH);
        assertFalse(getWithTimeout(exists).isPresent());
    }
}
