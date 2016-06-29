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
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * Abstract base class for {@link Request}s involving specific local history. This class is visible outside of this
 * package solely for the ability to perform a unified instanceof check.
 *
 * @author Robert Varga
 *
 * @param <T> Message type
 */
@Beta
public abstract class LocalHistoryRequest<T extends LocalHistoryRequest<T>> extends Request<LocalHistoryIdentifier, T> {
    private static final long serialVersionUID = 1L;

    LocalHistoryRequest(final LocalHistoryIdentifier target, final ActorRef replyTo) {
        super(target, replyTo);
        Preconditions.checkArgument(target.getHistoryId() != 0, "History identifier must be non-zero");
    }

    LocalHistoryRequest(final T request, final ABIVersion version) {
        super(request, version);
    }

    @Override
    public final LocalHistoryFailure toRequestFailure(final RequestException cause) {
        return new LocalHistoryFailure(getTarget(), cause);
    }

    @Override
    protected abstract AbstractLocalHistoryRequestProxy<T> externalizableProxy(final ABIVersion version);
}
