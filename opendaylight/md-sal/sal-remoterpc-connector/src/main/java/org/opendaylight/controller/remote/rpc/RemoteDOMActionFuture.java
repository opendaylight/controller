/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import java.util.concurrent.CompletionStage;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.remote.rpc.messages.ActionResponse;
import org.opendaylight.mdsal.dom.api.DOMActionException;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

final class RemoteDOMActionFuture extends AbstractRemoteFuture<Absolute, DOMRpcResult, DOMActionException> {
    RemoteDOMActionFuture(final @NonNull Absolute type, final @NonNull CompletionStage<Object> requestFuture) {
        super(type, requestFuture);
    }

    @Override
    DOMRpcResult processReply(final Object reply) {
        if (reply instanceof ActionResponse actionReply) {
            final ContainerNode output = actionReply.getOutput();
            return output == null ? new DefaultDOMRpcResult(actionReply.getErrors())
                    : new DefaultDOMRpcResult(output, actionReply.getErrors());
        }

        return null;
    }

    @Override
    Class<DOMActionException> exceptionClass() {
        return DOMActionException.class;
    }

    @Override
    DOMActionException wrapCause(final Throwable cause) {
        return new RemoteDOMActionException("Exception during invoking ACTION", cause);
    }
}
