/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

public final class CreateLocalHistoryFailure extends AbstractFailure<LocalHistoryIdentifier> {
    private static final class Proxy extends AbstractFailureProxy<LocalHistoryIdentifier> {
        private static final long serialVersionUID = 1L;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final LocalHistoryIdentifier identifier, final Exception cause) {
            super(identifier, cause);
        }

        @Override
        CreateLocalHistoryFailure readResolve() {
            return new CreateLocalHistoryFailure(getIdentifier(), getCause());
        }
    }

    private static final long serialVersionUID = 1L;

    public CreateLocalHistoryFailure(final LocalHistoryIdentifier historyId, final Exception cause) {
        super(historyId, cause);
    }

    @Override
    Proxy writeReplace() {
        return new Proxy(getIdentifier(), getCause());
    }
}
