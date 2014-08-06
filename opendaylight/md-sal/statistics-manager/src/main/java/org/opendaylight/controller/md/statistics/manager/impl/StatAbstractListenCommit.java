/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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

//    protected final Map<InstanceIdentifier<T>, Integer> repeaterMap = new ConcurrentHashMap<>();

    private ListenerRegistration<DataChangeListener> listenerRegistration;

    private final Class<T> clazz;

    /* Constructor has to make a registration */
    public StatAbstractListenCommit(final StatisticsManager manager, final DataBroker db,
            final NotificationProviderService nps, final Class<T> clazz) {
        super(manager,nps);
        this.clazz = Preconditions.checkNotNull(clazz, "Referenced Class can not be null");
        Preconditions.checkArgument(db != null, "DataBroker can not be null!");
        listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                getWildCardedRegistrationPath(), this, DataChangeScope.BASE);
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
        /* All DataObjects for create */
        final Map<InstanceIdentifier<?>, DataObject> createdData = changeEvent.getCreatedData() != null
                ? changeEvent.getCreatedData() : Collections.<InstanceIdentifier<?>, DataObject> emptyMap();
        /* All DataObjects for remove */
        final Set<InstanceIdentifier<?>> removeData = changeEvent.getRemovedPaths() != null
                ? changeEvent.getRemovedPaths() : Collections.<InstanceIdentifier<?>> emptySet();

        this.createData(createdData);
        this.removeData(removeData);
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
    }

    @SuppressWarnings("unchecked")
    private void createData(final Map<InstanceIdentifier<?>, DataObject> createdData) {
        final Set<InstanceIdentifier<?>> keys = createdData.keySet() != null
                ? createdData.keySet() : Collections.<InstanceIdentifier<?>> emptySet();
        for (final InstanceIdentifier<?> key : keys) {
            if (clazz.equals(key.getTargetType())) {
                final InstanceIdentifier<Node> nodeIdent =
                        key.firstIdentifierOf(Node.class);
                if (preConfigurationCheck(nodeIdent)) {
                    final Optional<DataObject> value = Optional.of(createdData.get(key));
                    if (value.isPresent()) {
                        try {
                            Thread.sleep(10);
                        }
                        catch (final InterruptedException e) {
                            LOG.trace("Statistic change listener sleep interupted.");
                        }
                        createStat((InstanceIdentifier<T>)key, (T)value.get(), nodeIdent);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void removeData(final Set<InstanceIdentifier<?>> removeData) {
        for (final InstanceIdentifier<?> key : removeData) {
            if (clazz.equals(key.getTargetType())) {
                removeStat((InstanceIdentifier<T>) key);
            }
        }
    }

    /**
     * PreConfigurationCheck - Node identified by input InstanceIdentifier<Node>
     * has to be registered in {@link org.opendaylight.controller.md.statistics.manager.StatPermCollector}
     *
     * @param InstanceIdentifier<Node> nodeIdent
     */
    private boolean preConfigurationCheck(final InstanceIdentifier<Node> nodeIdent) {
        Preconditions.checkNotNull(nodeIdent, "FlowCapableNode ident can not be null!");
        return manager.getStatCollector().isProvidedFlowNodeActive(nodeIdent);
    }

    /**
     * Method checks active connection for defined InstanceIdentifier<FlowCapableNode> node path
     * and notifies {@link org.opendaylight.controller.md.statistics.manager.StatPermCollector}
     * for collect next statistics. If the FlowCapableNode is connected, method calls delete
     * process fore every one node from provided list.
     *
     * @param boolean isResponseFromStatCollector - parameter which could prevent bad call
     *        (implemented mostly as orientation attention for don't make unwanted call)
     * @param List elemForDelete - all not updated element by collected statistic processing
     * @param InstanceIdentifier<Node> nodeIdent
     */
    protected void cleaningOperationalDS(final boolean isResponseFromStatCollector,
            final List<InstanceIdentifier<T>> elemForDelete, final InstanceIdentifier<Node> nodeIdent) {
        if ( ! preConfigurationCheck(nodeIdent) && isResponseFromStatCollector) {
            manager.getStatCollector().collectNextStatistics();
            return;
        }
        /* Devide update and delete trans */
        manager.getStatCollector().collectNextStatistics();
        for (final InstanceIdentifier<T> deleteElem : elemForDelete) {
            removeStat(deleteElem);
        }
    }
}

