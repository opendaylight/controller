/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.Status.Failure;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.remote.rpc.messages.ActionResponse;
import org.opendaylight.controller.remote.rpc.messages.ExecuteAction;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actor receiving invocation requests from remote nodes, routing them to
 * {@link DOMRpcService#invokeRpc(SchemaPath, NormalizedNode)} and
 * {@link DOMActionService#invokeAction(SchemaPath, DOMDataTreeIdentifier, ContainerNode)}.
 *
 * <p>Note that while the two interfaces are very similar, invocation strategies are slightly different due to historic
 * behavior of RPCs:
 * <ul>
 *   <li>RPCs allow both null input and output, and this is passed to the infrastructure. Furthermore any invocation
 *       which results in errors being reported drops the output content, even if it is present -- which is wrong, as
 *       'errors' in this case can also be just warnings.</li>
 *   <li>Actions do not allow null input, but allow null output. If the output is present, it is passed along with any
 *       errors reported.</li>
 * </ul>
 */
final class OpsInvoker extends AbstractUntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(OpsInvoker.class);

    private final DOMRpcService rpcService;
    private final DOMActionService actionService;

    private OpsInvoker(final String logName, final DOMRpcService rpcService, final DOMActionService actionService) {
        super(logName);
        this.rpcService = requireNonNull(rpcService);
        this.actionService = requireNonNull(actionService);
    }

    static Props props(final String logName, final DOMRpcService rpcService, final DOMActionService actionService) {
        return Props.create(OpsInvoker.class,
            requireNonNull(logName, "logName cannot be null"),
            requireNonNull(rpcService, "DOMRpcService cannot be null"),
            requireNonNull(actionService, "DOMActionService cannot be null"));
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public ActorRef getSender() {
        return super.getSender();
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof ExecuteRpc executeRpc) {
            LOG.debug("{}: Handling ExecuteOps Message", logName);
            execute(executeRpc);
        } else if (message instanceof ExecuteAction executeAction) {
            execute(executeAction);
        } else {
            unknownMessage(message);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void execute(final ExecuteRpc msg) {
        LOG.debug("{}: Executing RPC {}", logName, msg.getType());
        final ActorRef sender = getSender();

        final ListenableFuture<? extends DOMRpcResult> future;
        try {
            future = rpcService.invokeRpc(msg.getType(), msg.getInput());
        } catch (final RuntimeException e) {
            LOG.debug("{}: Failed to invoke RPC {}", logName, msg.getType(), e);
            sender.tell(new Failure(e), self());
            return;
        }

        Futures.addCallback(future, new AbstractCallback<QName, DOMRpcResult>(getSender(), msg.getType()) {
            @Override
            Object nullResponse(final QName type) {
                LOG.warn("{}: Execution of {} resulted in null result", logName, type);
                return new RpcResponse(null);
            }

            @Override
            Object response(final QName type, final DOMRpcResult result) {
                final Collection<? extends RpcError> errors = result.errors();
                return errors.isEmpty() ? new RpcResponse(result.value())
                        // This is legacy (wrong) behavior, which ignores the fact that errors may be just warnings,
                        // discarding any output
                        : new Failure(new RpcErrorsException(String.format("Execution of rpc %s failed", type),
                            errors));
            }
        }, MoreExecutors.directExecutor());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void execute(final ExecuteAction msg) {
        LOG.debug("{}: Executing Action {}", logName, msg.getType());

        final ActorRef sender = getSender();

        final ListenableFuture<? extends DOMRpcResult> future;
        try {
            future = actionService.invokeAction(msg.getType(), msg.getPath(), msg.getInput());
        } catch (final RuntimeException e) {
            LOG.debug("{}: Failed to invoke action {}", logName, msg.getType(), e);
            sender.tell(new Failure(e), self());
            return;
        }

        Futures.addCallback(future, new AbstractCallback<Absolute, DOMRpcResult>(getSender(), msg.getType()) {
            @Override
            Object nullResponse(final Absolute type) {
                throw new IllegalStateException("Null invocation result of action " + type);
            }

            @Override
            Object response(final Absolute type, final DOMRpcResult result) {
                final var errors = result.errors();
                return errors.isEmpty() ? new ActionResponse(result.value(), errors)
                    // This is legacy (wrong) behavior, which ignores the fact that errors may be just warnings,
                    // discarding any output
                    : new Failure(new RpcErrorsException(String.format("Execution of action %s failed", type),
                        errors));
            }
        }, MoreExecutors.directExecutor());
    }

    private abstract class AbstractCallback<T, R> implements FutureCallback<R> {
        private final ActorRef replyTo;
        private final T type;

        AbstractCallback(final ActorRef replyTo, final T type) {
            this.replyTo = requireNonNull(replyTo);
            this.type = requireNonNull(type);
        }

        @Override
        public final void onSuccess(final R result) {
            final Object response;
            if (result == null) {
                // This shouldn't happen but the FutureCallback annotates the result param with Nullable so handle null
                // here to avoid FindBugs warning.
                response = nullResponse(type);
            } else {
                response = response(type, result);
            }

            LOG.debug("{}, Sending response for execution of {} : {}", logName, type, response);
            replyTo.tell(response, self());
        }

        @Override
        public final void onFailure(final Throwable failure) {
            LOG.debug("{}: Failed to execute operation {}", logName, type, failure);
            LOG.error("{}: Failed to execute operation {} due to {}. More details are available on DEBUG level.",
                logName, type, Throwables.getRootCause(failure).getMessage());
            replyTo.tell(new Failure(failure), self());
        }

        abstract @NonNull Object nullResponse(@NonNull T type);

        abstract @NonNull Object response(@NonNull T type, @NonNull R result);
    }
}
