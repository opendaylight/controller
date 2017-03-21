/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.MockitoAnnotations.initMocks;

import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;

public class FailedDataTreeModificationTest {

    private FailedDataTreeModification failedDataTreeModification;

    @Mock
    private YangInstanceIdentifier instanceIdentifier;
    @Mock
    private NormalizedNode normalizedNode;
    @Mock
    private DataTreeModificationCursor dataTreeModificationCursor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        final Supplier supplier = () -> new RuntimeException("Operation failed.");
        failedDataTreeModification = new FailedDataTreeModification(supplier);
    }

    @Test(expected = RuntimeException.class)
    public void testReadNode() throws Exception {
        failedDataTreeModification.readNode(instanceIdentifier);
    }

    @Test(expected = RuntimeException.class)
    public void testNewModification() throws Exception {
        failedDataTreeModification.newModification();
    }

    @Test(expected = RuntimeException.class)
    public void testDelete() throws Exception {
        failedDataTreeModification.delete(instanceIdentifier);
    }

    @Test(expected = RuntimeException.class)
    public void testMerge() throws Exception {
        failedDataTreeModification.merge(instanceIdentifier, normalizedNode);
    }

    @Test(expected = RuntimeException.class)
    public void testWrite() throws Exception {
        failedDataTreeModification.write(instanceIdentifier, normalizedNode);
    }

    @Test(expected = RuntimeException.class)
    public void testReady() throws Exception {
        failedDataTreeModification.ready();
    }

    @Test(expected = RuntimeException.class)
    public void testApplyToCursor() throws Exception {
        failedDataTreeModification.applyToCursor(dataTreeModificationCursor);
    }

    @Test(expected = RuntimeException.class)
    public void testCreateCursor() throws Exception {
        failedDataTreeModification.createCursor(instanceIdentifier);
    }
}
