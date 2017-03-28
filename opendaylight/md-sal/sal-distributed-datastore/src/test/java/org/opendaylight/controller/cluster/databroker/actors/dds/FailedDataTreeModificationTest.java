/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class FailedDataTreeModificationTest {

    private FailedDataTreeModification modification;

    @Before
    public void setUp() throws Exception {
        modification = new FailedDataTreeModification(RuntimeException::new);
    }

    @Test(expected = RuntimeException.class)
    public void testReadNode() throws Exception {
        modification.readNode(YangInstanceIdentifier.EMPTY);
    }

    @Test(expected = RuntimeException.class)
    public void testNewModification() throws Exception {
        modification.newModification();
    }

    @Test(expected = RuntimeException.class)
    public void testDelete() throws Exception {
        modification.delete(YangInstanceIdentifier.EMPTY);
    }

    @Test(expected = RuntimeException.class)
    public void testMerge() throws Exception {
        modification.merge(YangInstanceIdentifier.EMPTY, null);
    }

    @Test(expected = RuntimeException.class)
    public void testWrite() throws Exception {
        modification.write(YangInstanceIdentifier.EMPTY, null);
    }

    @Test(expected = RuntimeException.class)
    public void testReady() throws Exception {
        modification.ready();
    }

    @Test(expected = RuntimeException.class)
    public void testApplyToCursor() throws Exception {
        modification.applyToCursor(null);
    }

    @Test(expected = RuntimeException.class)
    public void testCreateCursor() throws Exception {
        modification.createCursor(YangInstanceIdentifier.EMPTY);
    }

}