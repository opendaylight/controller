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
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;

/**
 * Generic {@link RequestFailure} involving a {@link LocalHistoryRequest}.
 */
public final class LocalHistoryFailure extends RequestFailure<LocalHistoryIdentifier, LocalHistoryFailure> {
    interface SerialForm extends RequestFailure.SerialForm<LocalHistoryIdentifier, LocalHistoryFailure> {
        @Override
        default LocalHistoryIdentifier readTarget(final DataInput in) throws IOException {
            return LocalHistoryIdentifier.readFrom(in);
        }

        @Override
        default LocalHistoryFailure createFailure(final LocalHistoryIdentifier target, final long sequence,
                final RequestException cause) {
            return new LocalHistoryFailure(target, sequence, cause);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private LocalHistoryFailure(final LocalHistoryFailure failure, final ABIVersion version) {
        super(failure, version);
    }

    LocalHistoryFailure(final LocalHistoryIdentifier target, final long sequence, final RequestException cause) {
        super(target, sequence, cause);
    }

    @Override
    protected LocalHistoryFailure cloneAsVersion(final ABIVersion targetVersion) {
        return new LocalHistoryFailure(this, targetVersion);
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return new HF(this);
    }
}
