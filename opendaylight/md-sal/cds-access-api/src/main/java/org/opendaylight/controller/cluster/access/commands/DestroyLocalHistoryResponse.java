/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

public final class DestroyLocalHistoryResponse extends AbstractLocalHistorySuccess {
    private static final class Proxy extends LocalHistorySuccessProxy {
        private static final long serialVersionUID = 1L;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final LocalHistoryIdentifier identifier) {
            super(identifier);
        }

        @Override
        DestroyLocalHistoryResponse readResolve() {
            return new DestroyLocalHistoryResponse(getIdentifier());
        }
    }

    private static final long serialVersionUID = 1L;

    public DestroyLocalHistoryResponse(final LocalHistoryIdentifier historyId) {
        super(historyId);
    }

    @Override
    Proxy writeReplace() {
        return new Proxy(getIdentifier());
    }
}
