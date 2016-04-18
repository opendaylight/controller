/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors;

import akka.actor.PoisonPill;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.UntypedPersistentActor;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

public final class FrontendActor extends UntypedPersistentActor {
    private static abstract class State {
        abstract void onReceiveCommand(Object command);
        abstract void onReceiveRecover(Object recover);
    }

    private final class Recovering extends State {
        private final long lastGeneration = 0;

        @Override
        void onReceiveCommand(final Object command) {
            // TODO Auto-generated method stub

        }

        @Override
        void onReceiveRecover(Object recover) {
            if (recover instanceof FrontendGenerationSnapshot) {

            } else if (recover instanceof RecoveryCompleted) {
                // FIXME: switch to SAVING
                saveSnapshot(new FrontendGenerationSnapshot(0));
                PartialFunction<Object, BoxedUnit> arg = null;
                FrontendActor.this.getContext().become(arg);
            }
        }


    }

    private static abstract class AbstractRecoveredState extends State {
        @Override
        void onReceiveRecover(Object recover) {
            // TODO Auto-generated method stub

        }
    }

    private final class Saving extends AbstractRecoveredState {
        @Override
        void onReceiveCommand(Object command) {
            if (command instanceof SaveSnapshotSuccess) {
                // FIXME
            } else if (command instanceof SaveSnapshotFailure) {
                getSelf().tell(PoisonPill.getInstance(), getSelf());
            }
        }
    }

    private final class Operational extends AbstractRecoveredState {

        @Override
        void onReceiveCommand(Object command) {
            // TODO Auto-generated method stub

        }


    }

    private static final String PERSISTENCE_ID = "frontend actor";
    private final State currentState = new Recovering();

    @Override
    public String persistenceId() {
        return PERSISTENCE_ID;
    }

    @Override
    public void onReceiveCommand(final Object command) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void onReceiveRecover(final Object recover) throws Exception {
    }

    @Override
    public void saveSnapshot(Object snapshot) {
        // TODO Auto-generated method stub
        super.saveSnapshot(snapshot);
    }


}
