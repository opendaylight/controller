/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Status;
import akka.actor.Status.Failure;
import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import akka.dispatch.Recover;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.CanCommit;
import org.opendaylight.controller.cluster.datastore.DataTreeCohortActor.Success;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

/**
 * Composite cohort, which coordinates multiple user-provided cohorts as if it was only one cohort.
 * <p/>
 * It tracks current operation and list of cohorts which successfuly finished previous phase in
 * case, if abort is necessary to invoke it only on cohort steps which are still active.
 *
 */
class CompositeDataTreeCohort {
    private static final Logger LOG = LoggerFactory.getLogger(CompositeDataTreeCohort.class);

    private enum State {
        /**
         * Cohorts are idle, no messages were sent.
         */
        IDLE,
        /**
         * CanCommit message was sent to all participating cohorts.
         */
        CAN_COMMIT_SENT,
        /**
         * Successful canCommit responses were received from every participating cohort.
         */
        CAN_COMMIT_SUCCESSFUL,
        /**
         * PreCommit message was sent to all participating cohorts.
         */
        PRE_COMMIT_SENT,
        /**
         * Successful preCommit responses were received from every participating cohort.
         */
        PRE_COMMIT_SUCCESSFUL,
        /**
         * Commit message was send to all participating cohorts.
         */
        COMMIT_SENT,
        /**
         * Successful commit responses were received from all participating cohorts.
         */
        COMMITED,
        /**
         * Some of cohorts responded back with unsuccessful message.
         */
        FAILED,
        /**
         * Abort message was send to all cohorts which responded with success previously.
         */
        ABORTED
    }

    static final Recover<Object> EXCEPTION_TO_MESSAGE = new Recover<Object>() {
        @Override
        public Failure recover(final Throwable error) {
            return new Failure(error);
        }
    };

    private final DataTreeCohortActorRegistry registry;
    private final TransactionIdentifier txId;
    private final SchemaContext schema;
    private final Timeout timeout;

    private List<Success> successfulFromPrevious;
    private State state = State.IDLE;

    CompositeDataTreeCohort(final DataTreeCohortActorRegistry registry, final TransactionIdentifier transactionID,
        final SchemaContext schema, final Timeout timeout) {
        this.registry = Preconditions.checkNotNull(registry);
        this.txId = Preconditions.checkNotNull(transactionID);
        this.schema = Preconditions.checkNotNull(schema);
        this.timeout = Preconditions.checkNotNull(timeout);
    }

    void reset() {
        switch (state) {
            case CAN_COMMIT_SENT:
            case CAN_COMMIT_SUCCESSFUL:
            case PRE_COMMIT_SENT:
            case PRE_COMMIT_SUCCESSFUL:
            case COMMIT_SENT:
                abort();
                break;
            case ABORTED:
            case COMMITED:
            case FAILED:
            case IDLE:
                break;
            default:
                throw new IllegalStateException("Unhandled state " + state);
        }

        successfulFromPrevious = null;
        state = State.IDLE;
    }

    void canCommit(final DataTreeCandidate tip) throws ExecutionException, TimeoutException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("{}: canCommit - candidate: {}", txId, tip);
        } else {
            LOG.debug("{}: canCommit - candidate rootPath: {}", txId, tip.getRootPath());
        }

        final List<CanCommit> messages = registry.createCanCommitMessages(txId, tip, schema);
        LOG.debug("{}: canCommit - messages: {}", txId, messages);
        if (messages.isEmpty()) {
            successfulFromPrevious = ImmutableList.of();
            changeStateFrom(State.IDLE, State.CAN_COMMIT_SUCCESSFUL);
            return;
        }

        final List<Entry<ActorRef, Future<Object>>> futures = new ArrayList<>(messages.size());
        for (CanCommit message : messages) {
            final ActorRef actor = message.getCohort();
            final Future<Object> future = Patterns.ask(actor, message, timeout).recover(EXCEPTION_TO_MESSAGE,
                ExecutionContexts.global());
            LOG.trace("{}: requesting canCommit from {}", txId, actor);
            futures.add(new SimpleImmutableEntry<>(actor, future));
        }

        changeStateFrom(State.IDLE, State.CAN_COMMIT_SENT);
        processResponses(futures, State.CAN_COMMIT_SENT, State.CAN_COMMIT_SUCCESSFUL);
    }

    void preCommit() throws ExecutionException, TimeoutException {
        LOG.debug("{}: preCommit - successfulFromPrevious: {}", txId, successfulFromPrevious);

        Preconditions.checkState(successfulFromPrevious != null);
        if (successfulFromPrevious.isEmpty()) {
            changeStateFrom(State.CAN_COMMIT_SUCCESSFUL, State.PRE_COMMIT_SUCCESSFUL);
            return;
        }

        final List<Entry<ActorRef, Future<Object>>> futures = sendMessageToSuccessful(
            new DataTreeCohortActor.PreCommit(txId));
        changeStateFrom(State.CAN_COMMIT_SUCCESSFUL, State.PRE_COMMIT_SENT);
        processResponses(futures, State.PRE_COMMIT_SENT, State.PRE_COMMIT_SUCCESSFUL);
    }

    void commit() throws ExecutionException, TimeoutException {
        LOG.debug("{}: commit - successfulFromPrevious: {}", txId, successfulFromPrevious);
        if (successfulFromPrevious.isEmpty()) {
            changeStateFrom(State.PRE_COMMIT_SUCCESSFUL, State.COMMITED);
            return;
        }

        Preconditions.checkState(successfulFromPrevious != null);
        final List<Entry<ActorRef, Future<Object>>> futures = sendMessageToSuccessful(
            new DataTreeCohortActor.Commit(txId));
        changeStateFrom(State.PRE_COMMIT_SUCCESSFUL, State.COMMIT_SENT);
        processResponses(futures, State.COMMIT_SENT, State.COMMITED);
    }

    Optional<List<Future<Object>>> abort() {
        LOG.debug("{}: abort - successfulFromPrevious: {}", txId, successfulFromPrevious);

        state = State.ABORTED;
        if (successfulFromPrevious == null || successfulFromPrevious.isEmpty()) {
            return Optional.empty();
        }

        final DataTreeCohortActor.Abort message = new DataTreeCohortActor.Abort(txId);
        final List<Future<Object>> futures = new ArrayList<>(successfulFromPrevious.size());
        for (Success s : successfulFromPrevious) {
            futures.add(Patterns.ask(s.getCohort(), message, timeout));
        }
        return Optional.of(futures);
    }

    private List<Entry<ActorRef, Future<Object>>> sendMessageToSuccessful(final Object message) {
        LOG.debug("{}: sendMesageToSuccessful: {}", txId, message);

        final List<Entry<ActorRef, Future<Object>>> ret = new ArrayList<>(successfulFromPrevious.size());
        for (Success s : successfulFromPrevious) {
            final ActorRef actor = s.getCohort();
            ret.add(new SimpleImmutableEntry<>(actor, Patterns.ask(actor, message, timeout)));
        }
        return ret;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void processResponses(final List<Entry<ActorRef, Future<Object>>> futures, final State currentState,
            final State afterState) throws TimeoutException, ExecutionException {
        LOG.debug("{}: processResponses - currentState: {}, afterState: {}", txId, currentState, afterState);

        final Iterable<Object> results;
        try {
            results = Await.result(Futures.sequence(Lists.transform(futures, e -> e.getValue()),
                ExecutionContexts.global()), timeout.duration());
        } catch (TimeoutException e) {
            successfulFromPrevious = null;
            LOG.debug("{}: processResponses - error from Future", txId, e);

            for (Entry<ActorRef, Future<Object>> f : futures) {
                if (!f.getValue().isCompleted()) {
                    LOG.info("{}: actor {} failed to respond", txId, f.getKey());
                }
            }
            throw e;
        } catch (ExecutionException e) {
            successfulFromPrevious = null;
            LOG.debug("{}: processResponses - error from Future", txId, e);
            throw e;
        } catch (Exception e) {
            successfulFromPrevious = null;
            LOG.debug("{}: processResponses - error from Future", txId, e);
            throw new ExecutionException(e);
        }

        final Collection<Failure> failed = new ArrayList<>(1);
        final List<Success> successful = new ArrayList<>(futures.size());
        for (Object result : results) {
            if (result instanceof DataTreeCohortActor.Success) {
                successful.add((Success) result);
            } else if (result instanceof Status.Failure) {
                failed.add((Failure) result);
            } else {
                LOG.warn("{}: unrecognized response {}, ignoring it", result);
            }
        }

        LOG.debug("{}: processResponses - successful: {}, failed: {}", txId, successful, failed);

        successfulFromPrevious = successful;
        if (!failed.isEmpty()) {
            changeStateFrom(currentState, State.FAILED);
            final Iterator<Failure> it = failed.iterator();
            final Throwable firstEx = it.next().cause();
            while (it.hasNext()) {
                firstEx.addSuppressed(it.next().cause());
            }
            Throwables.propagateIfInstanceOf(firstEx, ExecutionException.class);
            Throwables.propagateIfInstanceOf(firstEx, TimeoutException.class);
            throw new ExecutionException(firstEx);
        }
        changeStateFrom(currentState, afterState);
    }

    void changeStateFrom(final State expected, final State followup) {
        Preconditions.checkState(state == expected);
        state = followup;
    }
}
