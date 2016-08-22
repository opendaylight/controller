/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.japi.Procedure;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.DelegatingPersistentDataProvider;
import org.opendaylight.controller.cluster.PersistentDataProvider;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.PersistentPayload;

/**
 * The DelegatingPersistentDataProvider used by RaftActor to override the configured persistent provider to
 * persist ReplicatedLogEntry's based on whether or not the payload is a PersistentPayload instance.
 *
 * @author Thomas Pantelis
 */
class RaftActorDelegatingPersistentDataProvider extends DelegatingPersistentDataProvider {
    private final PersistentDataProvider persistentProvider;

    RaftActorDelegatingPersistentDataProvider(DataPersistenceProvider delegate,
            PersistentDataProvider persistentProvider) {
        super(delegate);
        this.persistentProvider = Preconditions.checkNotNull(persistentProvider);
    }

    @Override
    public <T> void persist(final T o, final Procedure<T> procedure) {
        if(getDelegate().isRecoveryApplicable()) {
            super.persist(o, procedure);
        } else {
            if(o instanceof ReplicatedLogEntry) {
                Payload payload = ((ReplicatedLogEntry)o).getData();
                if(payload instanceof PersistentPayload) {
                    // We persist the Payload but not the ReplicatedLogEntry to avoid gaps in the journal indexes
                    // on recovery if data persistence is later enabled.
                    persistentProvider.persist(payload, p -> procedure.apply(o));
                } else {
                    super.persist(o, procedure);
                }
            } else {
                super.persist(o, procedure);
            }
        }
    }
}
