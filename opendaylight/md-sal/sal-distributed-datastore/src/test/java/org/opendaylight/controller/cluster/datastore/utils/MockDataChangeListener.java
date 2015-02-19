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
            assertTrue(String.format("Change %d does not contain %s: %s", (i+1), expPaths[i], changeList.get(i)),
                    changeList.get(i).getCreatedData().containsKey(expPaths[i]));
        }
    }

    public void expectNoMoreChanges(String assertMsg) {
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
        assertEquals(assertMsg, expChangeEventCount, changeList.size());
    }
}
