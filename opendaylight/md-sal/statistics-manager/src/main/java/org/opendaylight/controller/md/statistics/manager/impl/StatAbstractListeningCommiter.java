/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.statistics.manager.StatListeningCommiter;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * ignore update (or why we need update ? ... is there some reason to have it?)
 *
 * create is important ... delete is only Operational reason
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 27, 2014
 */
public abstract class StatAbstractListeningCommiter<T extends DataObject> implements StatListeningCommiter<T> {

    private static final Logger LOG = LoggerFactory.getLogger(StatAbstractListeningCommiter.class);

    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private final Class<T> clazz;

    protected final StatisticsManager manager;

    /* Constructor has to make a registration */
    public StatAbstractListeningCommiter(final StatisticsManager manager, final DataBroker db, final Class<T> clazz) {
        this.clazz = Preconditions.checkNotNull(clazz, "Referenced Class can not be null");
        this.manager = Preconditions.checkNotNull(manager, "StatisticManager can not be null!");
        Preconditions.checkArgument(db != null, "DataBroker can not be null!");
        /* build path */
        final InstanceIdentifier<FlowCapableNode> wildCardedPath = InstanceIdentifier.create(Nodes.class)
                .child(Node.class).augmentation(FlowCapableNode.class);
        listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
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
                LOG.error("Error by stop {} StatListeningCommiter.", clazz.getSimpleName(), e);
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
                final Optional<DataObject> value = Optional.of(createdData.get(key));
                if (value.isPresent()) {
                    createStat((InstanceIdentifier<T>)key, (T)value.get());
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
}
