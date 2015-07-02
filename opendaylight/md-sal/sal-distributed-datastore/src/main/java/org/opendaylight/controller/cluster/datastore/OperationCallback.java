/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import java.util.concurrent.atomic.AtomicReference;

interface OperationCallback {
    OperationCallback NO_OP_CALLBACK = new OperationCallback() {
        @Override
        public void run() {
        }

        @Override
        public void success() {
        }

        @Override
        public void failure() {
        }

        @Override
        public void pause() {
        }

        @Override
        public void resume() {
        }
    };

    class Reference extends AtomicReference<OperationCallback> {
        private static final long serialVersionUID = 1L;

        public Reference(OperationCallback initialValue) {
            super(initialValue);
        }
    }

    void run();
    void pause();
    void resume();
    void success();
    void failure();
}
