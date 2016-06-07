/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

/**
 * Request to create a new local history.
 *
 * @author Robert Varga
 */
@Beta
public final class CreateLocalHistoryRequest extends LocalHistoryRequest<CreateLocalHistoryRequest> {
    private static final long serialVersionUID = 1L;

    public CreateLocalHistoryRequest(final LocalHistoryIdentifier target, final ActorRef replyTo) {
        super(target, replyTo);
    }

    private CreateLocalHistoryRequest(final CreateLocalHistoryRequest request, final ABIVersion version) {
        super(request, version);
    }

    @Override
    protected AbstractLocalHistoryRequestProxy<CreateLocalHistoryRequest> externalizableProxy(final ABIVersion version) {
        return new CreateLocalHistoryRequestProxyV1(this);
    }

    @Override
    protected CreateLocalHistoryRequest cloneAsVersion(final ABIVersion version) {
        return new CreateLocalHistoryRequest(this, version);
    }
}
