/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * A {@link DOMService} providing access to the conceptual data tree. Interactions
 * with the data tree are split into data producers and consumers (listeners). Each
 * of them operate on a set of subtrees, which need to be declared at instantiation time.
 *
 * Returned instances are not thread-safe and expected to be used by a single thread
 * at a time. Furthermore, producers may not be accessed from consumer callbacks
 * unless they were specified when the listener is registered.
 *
 * The service maintains a loop-free topology of producers and consumers. What this means
 * is that a consumer is not allowed to access a producer, which affects any of the
 * subtrees it is subscribed to. This restriction is in place to ensure the system does
 * not go into a feedback loop, where it is impossible to block either a producer or
 * a consumer without accumulating excess work in the backlog stemming from its previous
 * activity.
 */
public interface DOMDataTreeService extends DOMService {
    /**
     * Create a data producer which may write to specified conceptual subtrees.
     *
     * @param subtrees The set of subtrees the producer needs access to.
     * @param allowTxMerges Allow egress state compression. True if the producer
     *                      is allowed to merge transactions, should it find it
     *                      convenient.
     * @return A {@link DOMDataTreeProducer} instance.
     */
    @Nonnull DOMDataTreeProducer createProducer(@Nonnull Collection<DOMDataTreeIdentifier> subtrees, boolean allowTxMerges);

    /**
     * Register a {@link DOMDataTreeListener} instance. Once registered, the listener
     * will start receiving changes on the selected subtrees. If the listener cannot
     * keep up with the rate of changes, and allowRxMerges is set to true, this service
     * is free to merge the changes, so that a smaller number of them will be reported,
     * possibly hiding some data transitions (like flaps).
     *
     * If the listener wants to write into any producer, that producer has to be mentioned
     * in the call to this method. Those producers will be bound exclusively to the
     * registration, so that accessing it outside of this listener's callback will trigger
     * an error. Any producers mentioned must be idle, e.g. they may not have an open
     * transaction at the time this method is invoked.
     *
     * @param listener {@link DOMDataTreeListener} that is being registered
     * @param subtrees Conceptual subtree identifier of subtrees which should be monitored
     *                 for changes. May not be null or empty.
     * @param allowRxMerges True if the backend may perform ingress state compression.
     * @param producers {@link DOMDataTreeProducer} instances to bind to the listener.
     * @return A listener registration. Once closed, the listener will no longer be
     *         invoked and the producers will be unbound.
     * @throws IllegalArgumentException if subtrees is empty
     * @throws DOMDataTreeLoopException if the registration of the listener to the specified
     *                                  subtrees with specified producers would form a
     *                                  feedback loop
     */
    @Nonnull <T extends DOMDataTreeListener> ListenerRegistration<T> registerListener(@Nonnull T listener,
        @Nonnull Collection<DOMDataTreeIdentifier> subtrees, boolean allowRxMerges, @Nonnull Collection<DOMDataTreeProducer> producers);
}
