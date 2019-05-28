package org.opendaylight.controller.remote.rpc;

import akka.dispatch.OnComplete;
import com.google.common.util.concurrent.AbstractFuture;
import org.opendaylight.controller.remote.rpc.messages.ActionResponse;
import org.opendaylight.mdsal.dom.api.DOMActionException;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.spi.SimpleDOMActionResult;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

public class RemoteDOMActionFuture extends AbstractFuture<DOMActionResult> {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteDOMActionFuture.class);

    private final QName actionName;

    private RemoteDOMActionFuture(final QName rpcName) {
        this.actionName = requireNonNull(rpcName, "actionName");
    }

    public static RemoteDOMActionFuture create(final QName rpcName) {
        return new RemoteDOMActionFuture(rpcName);
    }

    protected void failNow(final Throwable error) {
        LOG.debug("Failing future {} for rpc {}", this, actionName, error);
        setException(error);
    }

    protected void completeWith(final Future<Object> future) {
        future.onComplete(new FutureUpdater(), ExecutionContext.Implicits$.MODULE$.global());
    }

    @Override
    public DOMActionResult get() throws InterruptedException, ExecutionException {
        try {
            return super.get();
        } catch (ExecutionException e) {
            throw mapException(e);
        }
    }

    @Override
    public DOMActionResult get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } catch (final ExecutionException e) {
            throw mapException(e);
        }
    }

    private static ExecutionException mapException(final ExecutionException ex) {
        final Throwable cause = ex.getCause();
        if (cause instanceof DOMActionException) {
            return ex;
        }
        return new ExecutionException(ex.getMessage(),
                new RemoteDOMActionException("Exception during invoking RPC", ex.getCause()));
    }

    private final class FutureUpdater extends OnComplete<Object> {

        @Override
        public void onComplete(final Throwable error, final Object reply) {
            if (error != null) {
                RemoteDOMActionFuture.this.failNow(error);
            } else if (reply instanceof ActionResponse) {
                final ActionResponse actionReply = (ActionResponse) reply;
                final Optional<ContainerNode> result = actionReply.getResultContainerNode();

                LOG.debug("Received response for rpc {}: result is {}", actionName, result);

                RemoteDOMActionFuture.this.set(new SimpleDOMActionResult(result.get(), Collections.emptyList()));

                LOG.debug("Future {} for rpc {} successfully completed", RemoteDOMActionFuture.this, actionName);
            } else {
                RemoteDOMActionFuture.this.failNow(new IllegalStateException("Incorrect reply type " + reply
                        + "from Akka"));
            }
        }
    }
}