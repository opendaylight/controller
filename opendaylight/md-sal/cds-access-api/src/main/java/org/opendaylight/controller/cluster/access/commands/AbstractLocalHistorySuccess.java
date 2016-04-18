/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Success;

abstract class AbstractLocalHistorySuccess extends AbstractMessage<LocalHistoryIdentifier, Success<LocalHistoryIdentifier>>
    implements LocalHistoryMessage {

    static abstract class LocalHistorySuccessProxy extends AbstractProxy<LocalHistoryIdentifier, Success<LocalHistoryIdentifier>> {
        private static final long serialVersionUID = 1L;

        public LocalHistorySuccessProxy() {
            // For Externalizable
        }

        LocalHistorySuccessProxy(final LocalHistoryIdentifier identifier) {
            super(identifier);
        }

        @Override
        abstract AbstractLocalHistorySuccess readResolve();
    }

    private static final long serialVersionUID = 1L;

    AbstractLocalHistorySuccess(final LocalHistoryIdentifier historyId) {
        super(historyId);
    }

    @Override
    public final LocalHistoryIdentifier getLocalHistoryIdentifier() {
        return getIdentifier();
    }

    @Override
    abstract LocalHistorySuccessProxy writeReplace();
}
