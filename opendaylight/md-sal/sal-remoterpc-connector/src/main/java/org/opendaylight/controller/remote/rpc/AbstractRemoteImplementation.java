/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.opendaylight.controller.remote.rpc.messages.AbstractExecute;
import scala.concurrent.Future;

/**
 * An abstract base class for remote RPC/action implementations.
 */
abstract class AbstractRemoteImplementation<T extends AbstractExecute<?, ?>> {
    // 0 for local, 1 for binding, 2 for remote
    static final long COST = 2;

    private final ActorRef remoteInvoker;
    private final Timeout askDuration;

    AbstractRemoteImplementation(final ActorRef remoteInvoker, final RemoteOpsProviderConfig config) {
        this.remoteInvoker = requireNonNull(remoteInvoker);
        this.askDuration = config.getAskDuration();
    }

    final Future<Object> ask(final T message) {
        return Patterns.ask(remoteInvoker, requireNonNull(message), askDuration);
    }
}
