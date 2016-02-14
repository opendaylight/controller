/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

public class MockDataTreeChangeListener implements DOMDataTreeChangeListener {

    private final List<Collection<DataTreeCandidate>> changeList =
            Lists.<Collection<DataTreeCandidate>>newArrayList();

    private volatile CountDownLatch changeLatch;
    private int expChangeEventCount;

    public MockDataTreeChangeListener(int expChangeEventCount) {
        reset(expChangeEventCount);
    }

    public void reset(int expChangeEventCount) {
        changeLatch = new CountDownLatch(expChangeEventCount);
        this.expChangeEventCount = expChangeEventCount;
        synchronized(changeList) {
            changeList.clear();
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeCandidate> changes) {
        synchronized(changeList) {
            changeList.add(changes);
        }
        changeLatch.countDown();
    }

    public void waitForChangeEvents() {
        boolean done = Uninterruptibles.awaitUninterruptibly(changeLatch, 5, TimeUnit.SECONDS);
        if(!done) {
            fail(String.format("Missing change notifications. Expected: %d. Actual: %d",
                    expChangeEventCount, (expChangeEventCount - changeLatch.getCount())));
        }
    }

    public void verifyNotifiedData(YangInstanceIdentifier... paths) {
        Set<YangInstanceIdentifier> pathSet = new HashSet<>(Arrays.asList(paths));
        synchronized(changeList) {
            for(Collection<DataTreeCandidate> list: changeList) {
                for(DataTreeCandidate c: list) {
                    pathSet.remove(c.getRootPath());
                }
            }
        }

        if(!pathSet.isEmpty()) {
            fail(pathSet + " not present in " + changeList);
        }
    }

    public void expectNoMoreChanges(String assertMsg) {
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        synchronized(changeList) {
            assertEquals(assertMsg, expChangeEventCount, changeList.size());
        }
    }

    public void verifyNoNotifiedData(YangInstanceIdentifier... paths) {
        Set<YangInstanceIdentifier> pathSet = new HashSet<>(Arrays.asList(paths));
        synchronized(changeList) {
            for(Collection<DataTreeCandidate> list: changeList) {
                for(DataTreeCandidate c: list) {
                    assertFalse("Unexpected " + c.getRootPath() + " present in DataTreeCandidate",
                            pathSet.contains(c.getRootPath()));
                }
            }
        }
    }
}
