/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * A mock DataChangeListener implementation.
 *
 * @author Thomas Pantelis
 */
public class MockDataChangeListener implements
                         AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> {

    private final List<AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>>> changeList =
            Collections.synchronizedList(Lists.<AsyncDataChangeEvent<YangInstanceIdentifier,
                                                NormalizedNode<?, ?>>>newArrayList());

    private volatile CountDownLatch changeLatch;
    private int expChangeEventCount;

    public MockDataChangeListener(int expChangeEventCount) {
        reset(expChangeEventCount);
    }

    public void reset(int expChangeEventCount) {
        changeLatch = new CountDownLatch(expChangeEventCount);
        this.expChangeEventCount = expChangeEventCount;
        changeList.clear();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        changeList.add(change);
        changeLatch.countDown();
    }

    public void waitForChangeEvents(YangInstanceIdentifier... expPaths) {
        boolean done = Uninterruptibles.awaitUninterruptibly(changeLatch, 5, TimeUnit.SECONDS);
        if(!done) {
            fail(String.format("Missing change notifications. Expected: %d. Actual: %d",
                    expChangeEventCount, (expChangeEventCount - changeLatch.getCount())));
        }

        for(int i = 0; i < expPaths.length; i++) {
            Map<YangInstanceIdentifier, NormalizedNode<?, ?>> createdData = changeList.get(i).getCreatedData();
            assertTrue(String.format("Change %d does not contain %s. Actual: %s", (i+1), expPaths[i], createdData),
                    createdData.containsKey(expPaths[i]));
        }
    }

    public NormalizedNode<?, ?> getCreatedData(int i, YangInstanceIdentifier path) {
        return changeList.get(i).getCreatedData().get(path);
    }

    public void verifyCreatedData(int i, YangInstanceIdentifier path) {
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> createdData = changeList.get(i).getCreatedData();
        assertTrue(path + " not present in " + createdData.keySet(), createdData.get(path) != null);
    }

    public void expectNoMoreChanges(String assertMsg) {
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        assertEquals(assertMsg, expChangeEventCount, changeList.size());
    }

    public void verifyNoCreatedData(int i, YangInstanceIdentifier path) {
        Map<YangInstanceIdentifier, NormalizedNode<?, ?>> createdData = changeList.get(i).getCreatedData();
        assertTrue("Unexpected " + path + " present in createdData", createdData.get(path) == null);
    }
}
