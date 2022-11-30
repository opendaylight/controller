/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import java.io.ObjectInput;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

/**
 * Request to create a new local history.
 */
public final class CreateLocalHistoryRequest extends LocalHistoryRequest<CreateLocalHistoryRequest> {
    interface SerialForm extends LocalHistoryRequest.SerialForm<CreateLocalHistoryRequest> {
        @Override
        default CreateLocalHistoryRequest readExternal(final ObjectInput in, final LocalHistoryIdentifier target,
                final long sequence, final ActorRef replyTo) {
            return new CreateLocalHistoryRequest(target, sequence, replyTo);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public CreateLocalHistoryRequest(final LocalHistoryIdentifier target, final ActorRef replyTo) {
        this(target, 0, replyTo);
    }

    CreateLocalHistoryRequest(final LocalHistoryIdentifier target, final long sequence, final ActorRef replyTo) {
        super(target, sequence, replyTo);
    }

    private CreateLocalHistoryRequest(final CreateLocalHistoryRequest request, final ABIVersion version) {
        super(request, version);
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return ABIVersion.MAGNESIUM.lt(version) ? new CHR(this) : new CreateLocalHistoryRequestProxyV1(this);
    }

    @Override
    protected CreateLocalHistoryRequest cloneAsVersion(final ABIVersion version) {
        return new CreateLocalHistoryRequest(this, version);
    }
}
