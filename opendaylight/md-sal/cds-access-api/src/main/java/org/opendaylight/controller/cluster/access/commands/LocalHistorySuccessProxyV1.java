/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.concepts.AbstractSuccessProxy;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

/**
 * Serialization proxy associated with {@link LocalHistorySuccess}.
 *
 * @author Robert Varga
 */
final class LocalHistorySuccessProxyV1 extends AbstractSuccessProxy<LocalHistoryIdentifier, LocalHistorySuccess> {
    private static final long serialVersionUID = 1L;

    LocalHistorySuccessProxyV1() {
        // For Externalizable
    }

    LocalHistorySuccessProxyV1(final LocalHistorySuccess success) {
        super(success);
    }

    @Override
    protected final LocalHistoryIdentifier readTarget(final DataInput in) throws IOException {
        return LocalHistoryIdentifier.readFrom(in);
    }

    @Override
    protected LocalHistorySuccess createSuccess(final LocalHistoryIdentifier target) {
        return new LocalHistorySuccess(target);
    }
}
