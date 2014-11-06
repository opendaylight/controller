/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.statistics.manager.StatListeningCommiter;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * StatAbstractListeneningCommiter
 * Class is abstract implementation for all Configuration/DataStore DataChange
 * listenable DataObjects like flows, groups, meters. It is a holder for common
 * functionality needed by construction/destruction class and for DataChange
 * event processing.
 *
 */
public abstract class StatAbstractListenCommit<T extends DataObject, N extends NotificationListener>
                                            extends StatAbstractNotifyCommit<N> implements StatListeningCommiter<T,N> {

    private static final Logger LOG = LoggerFactory.getLogger(StatAbstractListenCommit.class);

    private ListenerRegistration<DataChangeListener> listenerRegistration;

    protected final Map<InstanceIdentifier<Node>, Map<InstanceIdentifier<T>, Integer>> mapNodesForDelete = new ConcurrentHashMap<>();

    private final Class<T> clazz;

    private final DataBroker dataBroker;

    private volatile ReadOnlyTransaction currentReadTx;

    /* Constructor has to make a registration */
    public StatAbstractListenCommit(final StatisticsManager manager, final DataBroker db,
            final NotificationProviderService nps, final Class<T> clazz) {
        super(manager,nps);
        this.clazz = Preconditions.checkNotNull(clazz, "Referenced Class can not be null");
        Preconditions.checkArgument(db != null, "DataBroker can not be null!");
        listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                getWildCardedRegistrationPath(), this, DataChangeScope.BASE);
        this.dataBroker = db;
    }

    /**
     * Method returns WildCarded Path which is used for registration as a listening path changes in
     * {@link org.opendaylight.controller.md.sal.binding.api.DataChangeListener}
     * @return
     */
    protected abstract InstanceIdentifier<T> getWildCardedRegistrationPath();

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent) {
        Preconditions.checkNotNull(changeEvent,"Async ChangeEvent can not be null!");
        /*
         * If we have opened read transaction for configuraiton data store,
         * we will close and null it.
         *
         * Latest read transaction will be allocated on another read using readLatestConfiguration
         */
        if(currentReadTx != null) {
            final ReadOnlyTransaction previous = currentReadTx;
            currentReadTx = null;
            previous.close();
        }
    }

    @SuppressWarnings("unchecked")
    protected void removeData(final InstanceIdentifier<?> key, final Integer value) {
        if (clazz.equals(key.getTargetType())) {
            final InstanceIdentifier<Node> nodeIdent = key.firstIdentifierOf(Node.class);
            Map<InstanceIdentifier<T>, Integer> map = null;
            if (mapNodesForDelete.containsKey(nodeIdent)) {
                map = mapNodesForDelete.get(nodeIdent);
            }
            if (map == null) {
                map = new ConcurrentHashMap<>();
                mapNodesForDelete.put(nodeIdent, map);
            }
            map.put((InstanceIdentifier<T>) key, value);
        }
    }

    @Override
    public void cleanForDisconnect(final InstanceIdentifier<Node> nodeIdent) {
        mapNodesForDelete.remove(nodeIdent);
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception e) {
                LOG.error("Error by stop {} DataChange StatListeningCommiter.", clazz.getSimpleName(), e);
            }
            listenerRegistration = null;
        }

        super.close();
    }

    protected final <K extends DataObject> Optional<K> readLatestConfiguration(final InstanceIdentifier<K> path) {
        if(currentReadTx == null) {
             currentReadTx = dataBroker.newReadOnlyTransaction();
        }
        try {
            return currentReadTx.read(LogicalDatastoreType.CONFIGURATION, path).checkedGet();
        } catch (final ReadFailedException e) {
            return Optional.absent();
        }
    }
}

