/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.common.actor;

import akka.japi.Procedure;
import akka.persistence.SnapshotSelectionCriteria;
import akka.persistence.UntypedPersistentActor;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUntypedPersistentActor extends UntypedPersistentActor {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    public AbstractUntypedPersistentActor() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Actor created {}", getSelf());
        }
        getContext().
            system().
            actorSelection("user/termination-monitor").
            tell(new Monitor(getSelf()), getSelf());

    }


    @Override public void onReceiveCommand(Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();
        if(LOG.isDebugEnabled()) {
            LOG.debug("Received message {}", messageType);
        }
        handleCommand(message);
        if(LOG.isDebugEnabled()) {
            LOG.debug("Done handling message {}", messageType);
        }

    }

    @Override public void onReceiveRecover(Object message) throws Exception {
        final String messageType = message.getClass().getSimpleName();
        if(LOG.isDebugEnabled()) {
            LOG.debug("Received message {}", messageType);
        }
        handleRecover(message);
        if(LOG.isDebugEnabled()) {
            LOG.debug("Done handling message {}", messageType);
        }

    }

    protected abstract void handleRecover(Object message) throws Exception;

    protected abstract void handleCommand(Object message) throws Exception;

    protected void ignoreMessage(Object message) {
        LOG.debug("Unhandled message {} ", message);
    }

    protected void unknownMessage(Object message) throws Exception {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Received unhandled message {}", message);
        }
        unhandled(message);
    }

    protected class PersistentDataProvider implements DataPersistenceProvider {

        public PersistentDataProvider(){

        }

        @Override
        public boolean isRecoveryApplicable() {
            return true;
        }

        @Override
        public <T> void persist(T o, Procedure<T> procedure) {
            AbstractUntypedPersistentActor.this.persist(o, procedure);
        }

        @Override
        public void saveSnapshot(Object o) {
            AbstractUntypedPersistentActor.this.saveSnapshot(o);
        }

        @Override
        public void deleteSnapshots(SnapshotSelectionCriteria criteria) {
            AbstractUntypedPersistentActor.this.deleteSnapshots(criteria);
        }

        @Override
        public void deleteMessages(long sequenceNumber) {
            AbstractUntypedPersistentActor.this.deleteMessages(sequenceNumber);
        }
    }

    protected class NonPersistentDataProvider implements DataPersistenceProvider {

        public NonPersistentDataProvider(){

        }

        @Override
        public boolean isRecoveryApplicable() {
            return false;
        }

        @Override
        public <T> void persist(T o, Procedure<T> procedure) {
            try {
                procedure.apply(o);
            } catch (Exception e) {
                LOG.error("An unexpected error occurred", e);
            }
        }

        @Override
        public void saveSnapshot(Object o) {
        }

        @Override
        public void deleteSnapshots(SnapshotSelectionCriteria criteria) {

        }

        @Override
        public void deleteMessages(long sequenceNumber) {

        }
    }
}
