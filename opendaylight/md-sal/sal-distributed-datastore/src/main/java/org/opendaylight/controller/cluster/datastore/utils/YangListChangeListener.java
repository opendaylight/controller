/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public abstract class YangListChangeListener implements
        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?,?>>{

    private final YangInstanceIdentifier listPath;
    private Collection<MapEntryNode> entries = Collections.EMPTY_LIST;
    private boolean initialEvent = true;

    protected YangListChangeListener(YangInstanceIdentifier listPath){
        this.listPath = Preconditions.checkNotNull(listPath, "listPath should not be null");
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {

        if(change.getUpdatedSubtree() instanceof MapNode){
            MapNode node = (MapNode) change.getUpdatedSubtree();
            entries = node.getValue();
        }

        for(Map.Entry<YangInstanceIdentifier, NormalizedNode<?,?>> entry : change.getCreatedData().entrySet()){
            if(!listPath.equals(entry.getKey())){
                entryAdded(entry.getKey(), entry.getValue());
            } else if(initialEvent){
                MapNode mapNode = (MapNode) entry.getValue();
                Collection<MapEntryNode> entries = mapNode.getValue();
                for(MapEntryNode e : entries){
                    entryAdded(entry.getKey().node(e.getIdentifier()), e);
                }
                initialEvent = false;
            }
        }

        for(YangInstanceIdentifier identifier : change.getRemovedPaths()){
            entryRemoved(identifier);
        }

    }

    public static ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>
        registerYangListChangeListener(ShardDataTree dataTree,
                                   YangInstanceIdentifier listPath,
                                   YangListChangeListener listener){

        Map.Entry<ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>, DOMImmutableDataChangeEvent> entry
                = dataTree.registerChangeListener(listPath, listener, AsyncDataBroker.DataChangeScope.ONE);
        ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> registration
                = entry.getKey();
        if(entry.getValue() != null) {
            listener.onDataChanged(entry.getValue());
        }
        return registration;
    }

    protected abstract void entryAdded(YangInstanceIdentifier key, NormalizedNode<?,?> value);

    protected abstract void entryRemoved(YangInstanceIdentifier key);

    protected long listSize(){
        return entries.size();
    }

    protected Collection<MapEntryNode> entries(){
        return entries;
    }

}
