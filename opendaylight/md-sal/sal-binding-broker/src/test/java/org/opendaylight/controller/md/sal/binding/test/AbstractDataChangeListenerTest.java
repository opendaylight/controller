/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.util.concurrent.SettableFuture;

public abstract class AbstractDataChangeListenerTest extends AbstractDataBrokerTest {

    protected static final class TestListener implements DataChangeListener {

        private final SettableFuture<AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject>> event;
        private boolean capture = false;

        private TestListener() {
            event = SettableFuture.create();
        }

        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> arg) {
            if (capture) {
                event.set(arg);
            }
        }

        public AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event() {
            try {
                return event.get(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new IllegalStateException(e);
            }
        }

        public boolean hasEvent() {
            return event.isDone();
        }

        public void startCapture() {
            this.capture = true;
        }
    }

    protected final TestListener createListener(final LogicalDatastoreType store, final InstanceIdentifier<?> path,
            final DataChangeScope scope) {
        TestListener listener = new TestListener();
        getDataBroker().registerDataChangeListener(store, path, listener, scope);
        listener.startCapture();
        return listener;
    }
}
