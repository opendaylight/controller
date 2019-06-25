/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.remote.rpc.messages.ActionResponse;
import org.opendaylight.mdsal.dom.api.DOMActionException;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.spi.SimpleDOMActionResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RemoteDOMActionFuture extends AbstractRemoteFuture<DOMActionResult> {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteDOMActionFuture.class);

    RemoteDOMActionFuture(final @NonNull SchemaPath type) {
        super(type);
    }

    @Override
    ExecutionException mapException(final ExecutionException ex) {
        final Throwable cause = ex.getCause();
        if (cause instanceof DOMActionException) {
            return ex;
        }
        return new ExecutionException(ex.getMessage(),
                new RemoteDOMActionException("Exception during invoking ACTION", cause));
    }

    @Override
    AbstractFutureUpdater newFutureUpdater() {
        return new AbstractFutureUpdater() {
            @Override
            boolean onComplete(final SchemaPath type, final Object reply) {
                if (reply instanceof ActionResponse) {
                    final ActionResponse actionReply = (ActionResponse) reply;
                    final ContainerNode output = actionReply.getOutput();
                    final SimpleDOMActionResult result = output == null
                            ? new SimpleDOMActionResult(actionReply.getErrors())
                                    : new SimpleDOMActionResult(output, actionReply.getErrors());
                    LOG.debug("Received response for action {}: result is {}", type, result);

                    RemoteDOMActionFuture.this.set(result);
                    LOG.debug("Future {} for action {} successfully completed", RemoteDOMActionFuture.this, type);
                    return true;
                }

                return false;
            }
        };
    }
}
