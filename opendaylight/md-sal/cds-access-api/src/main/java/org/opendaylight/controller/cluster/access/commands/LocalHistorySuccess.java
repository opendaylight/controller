/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.AbstractSuccessProxy;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;

/**
 * Success class for {@link RequestSuccess}es involving a specific local history.
 *
 * @author Robert Varga
 */
@Beta
public final class LocalHistorySuccess extends RequestSuccess<LocalHistoryIdentifier, LocalHistorySuccess> {
    private static final long serialVersionUID = 1L;

    public LocalHistorySuccess(final LocalHistoryIdentifier target, final long sequence) {
        super(target, sequence);
    }

    private LocalHistorySuccess(final LocalHistorySuccess success, final ABIVersion version) {
        super(success, version);
    }

    @Override
    protected LocalHistorySuccess cloneAsVersion(final ABIVersion version) {
        return new LocalHistorySuccess(this, version);
    }

    @Override
    protected AbstractSuccessProxy<LocalHistoryIdentifier, LocalHistorySuccess> externalizableProxy(
            final ABIVersion version) {
        return new LocalHistorySuccessProxyV1(this);
    }
}
