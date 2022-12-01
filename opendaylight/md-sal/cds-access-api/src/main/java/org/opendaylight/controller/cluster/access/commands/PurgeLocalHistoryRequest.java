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
 * Request to purge a local history. This request is sent by the client once it receives a successful reply to
 * {@link DestroyLocalHistoryRequest} and indicates it has removed all state attached to a particular local history.
 */
public final class PurgeLocalHistoryRequest extends LocalHistoryRequest<PurgeLocalHistoryRequest> {
    interface SerialForm extends LocalHistoryRequest.SerialForm<PurgeLocalHistoryRequest> {
        @Override
        default PurgeLocalHistoryRequest readExternal(final ObjectInput in, final LocalHistoryIdentifier target,
                final long sequence, final ActorRef replyTo) {
            return new PurgeLocalHistoryRequest(target, sequence, replyTo);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public PurgeLocalHistoryRequest(final LocalHistoryIdentifier target, final long sequence, final ActorRef replyTo) {
        super(target, sequence, replyTo);
    }

    private PurgeLocalHistoryRequest(final PurgeLocalHistoryRequest request, final ABIVersion version) {
        super(request, version);
    }

    @Override
    protected SerialForm externalizableProxy(final ABIVersion version) {
        return new PHR(this);
    }

    @Override
    protected PurgeLocalHistoryRequest cloneAsVersion(final ABIVersion version) {
        return new PurgeLocalHistoryRequest(this, version);
    }
}
