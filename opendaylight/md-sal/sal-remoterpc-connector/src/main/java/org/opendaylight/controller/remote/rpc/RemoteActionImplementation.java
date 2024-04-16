/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.remote.rpc.messages.ExecuteAction;
import org.opendaylight.mdsal.dom.api.DOMActionImplementation;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DOMActionImplementation} which routes invocation requests to a remote invoker actor.
 */
final class RemoteActionImplementation extends AbstractRemoteImplementation<ExecuteAction>
        implements DOMActionImplementation {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteActionImplementation.class);

    RemoteActionImplementation(final ActorRef remoteInvoker, final RemoteOpsProviderConfig config) {
        super(remoteInvoker, config);
    }

    /**
     * Routes action request to a remote invoker, which will execute the action and return with result.
     */
    @Override
    public ListenableFuture<DOMRpcResult> invokeAction(final Absolute type, final DOMDataTreeIdentifier path,
            final ContainerNode input) {
        LOG.debug("invoking action {} with path {}", type, path);
        return new RemoteDOMActionFuture(type, ask(ExecuteAction.from(type, path, input)));
    }

    @Override
    public long invocationCost() {
        return COST;
    }
}
