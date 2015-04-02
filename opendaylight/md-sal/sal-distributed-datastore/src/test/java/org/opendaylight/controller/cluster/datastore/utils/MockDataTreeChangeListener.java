package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MockDataTreeChangeListener implements DOMDataTreeChangeListener {

    private final List<Collection<DataTreeCandidate>> changeList =
            Collections.synchronizedList(Lists.<Collection<DataTreeCandidate>>newArrayList());

    private volatile CountDownLatch changeLatch;
    private int expChangeEventCount;

    public MockDataTreeChangeListener(int expChangeEventCount) {
        reset(expChangeEventCount);
    }

    public void reset(int expChangeEventCount) {
        changeLatch = new CountDownLatch(expChangeEventCount);
        this.expChangeEventCount = expChangeEventCount;
        changeList.clear();
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeCandidate> changes) {
        changeList.add(changes);
        changeLatch.countDown();
    }

    public void waitForChangeEvents() {
        boolean done = Uninterruptibles.awaitUninterruptibly(changeLatch, 5, TimeUnit.SECONDS);
        if(!done) {
            fail(String.format("Missing change notifications. Expected: %d. Actual: %d",
                    expChangeEventCount, (expChangeEventCount - changeLatch.getCount())));
        }
    }

    public void expectNoMoreChanges(String assertMsg) {
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
        assertEquals(assertMsg, expChangeEventCount, changeList.size());
    }
}
