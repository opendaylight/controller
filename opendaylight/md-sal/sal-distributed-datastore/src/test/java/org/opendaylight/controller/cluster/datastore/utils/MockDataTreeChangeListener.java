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

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

public class MockDataTreeChangeListener implements DOMDataTreeChangeListener {

    private final List<DataTreeCandidate> changeList = new ArrayList<>();

    private volatile CountDownLatch changeLatch;
    private int expChangeEventCount;

    public MockDataTreeChangeListener(final int expChangeEventCount) {
        reset(expChangeEventCount);
    }

    public void reset(final int newExpChangeEventCount) {
        changeLatch = new CountDownLatch(newExpChangeEventCount);
        this.expChangeEventCount = newExpChangeEventCount;
        synchronized (changeList) {
            changeList.clear();
        }
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeCandidate> changes) {
        if (changeLatch.getCount() > 0) {
            synchronized (changeList) {
                changeList.addAll(changes);
            }
            changeLatch.countDown();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void waitForChangeEvents(final YangInstanceIdentifier... expPaths) {
        boolean done = Uninterruptibles.awaitUninterruptibly(changeLatch, 5, TimeUnit.SECONDS);
        if (!done) {
            fail(String.format("Missing change notifications. Expected: %d. Actual: %d",
                    expChangeEventCount, expChangeEventCount - changeLatch.getCount()));
        }

        for (int i = 0; i < expPaths.length; i++) {
            final DataTreeCandidate candidate = changeList.get(i);
            final Optional<NormalizedNode<?, ?>> maybeDataAfter = candidate.getRootNode().getDataAfter();
            if (!maybeDataAfter.isPresent()) {
                fail(String.format("Change %d does not contain data after. Actual: %s", i + 1,
                        candidate.getRootNode()));
            }

            final NormalizedNode<?, ?> dataAfter = maybeDataAfter.get();
            final Optional<YangInstanceIdentifier> relativePath = expPaths[i].relativeTo(candidate.getRootPath());
            if (!relativePath.isPresent()) {
                assertEquals(String.format("Change %d does not contain %s. Actual: %s", i + 1, expPaths[i],
                        dataAfter), expPaths[i].getLastPathArgument(), dataAfter.getIdentifier());
            } else {
                NormalizedNode<?, ?> nextChild = dataAfter;
                for (PathArgument pathArg: relativePath.get().getPathArguments()) {
                    boolean found = false;
                    if (nextChild instanceof NormalizedNodeContainer) {
                        Optional<NormalizedNode<?, ?>> maybeChild = ((NormalizedNodeContainer)nextChild)
                                .getChild(pathArg);
                        if (maybeChild.isPresent()) {
                            found = true;
                            nextChild = maybeChild.get();
                        }
                    }

                    if (!found) {
                        fail(String.format("Change %d does not contain %s. Actual: %s", i + 1, expPaths[i], dataAfter));
                    }
                }
            }
        }
    }

    public void verifyNotifiedData(final YangInstanceIdentifier... paths) {
        Set<YangInstanceIdentifier> pathSet = new HashSet<>(Arrays.asList(paths));
        synchronized (changeList) {
            for (DataTreeCandidate c : changeList) {
                pathSet.remove(c.getRootPath());
            }
        }

        if (!pathSet.isEmpty()) {
            fail(pathSet + " not present in " + changeList);
        }
    }

    public void expectNoMoreChanges(final String assertMsg) {
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        synchronized (changeList) {
            assertEquals(assertMsg, expChangeEventCount, changeList.size());
        }
    }

    public void verifyNoNotifiedData(final YangInstanceIdentifier... paths) {
        Set<YangInstanceIdentifier> pathSet = new HashSet<>(Arrays.asList(paths));
        synchronized (changeList) {
            for (DataTreeCandidate c : changeList) {
                assertFalse("Unexpected " + c.getRootPath() + " present in DataTreeCandidate",
                        pathSet.contains(c.getRootPath()));
            }
        }
    }
}
