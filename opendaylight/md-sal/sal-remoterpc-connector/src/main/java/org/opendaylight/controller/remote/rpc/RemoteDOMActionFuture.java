/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.remote.rpc.messages.ActionResponse;
import org.opendaylight.mdsal.dom.api.DOMActionException;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.spi.SimpleDOMActionResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import scala.concurrent.Future;

final class RemoteDOMActionFuture extends AbstractRemoteFuture<Absolute, DOMActionResult, DOMActionException> {
    RemoteDOMActionFuture(final @NonNull Absolute type, final @NonNull Future<Object> requestFuture) {
        super(type, requestFuture);
    }

    @Override
    DOMActionResult processReply(final Object reply) {
        if (reply instanceof ActionResponse actionReply) {
            final ContainerNode output = actionReply.getOutput();
            return output == null ? new SimpleDOMActionResult(actionReply.getErrors())
                    : new SimpleDOMActionResult(output, actionReply.getErrors());
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
