/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.topology

import java.util.Collections
import java.util.List
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.common.RpcResult
import org.slf4j.LoggerFactory

class TopologyTransaction implements DataCommitTransaction<InstanceIdentifier<?extends DataObject>, DataObject> {
    static val LOG = LoggerFactory.getLogger(TopologyTransaction);
    @Property
    val DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;
    
    @Property
    IPluginOutTopologyService topologyPublisher;
    
    @Property
    DataProviderService dataService;
    @Property
    List<TopoEdgeUpdate> edgeUpdates;
    
    new(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification,IPluginOutTopologyService topologyPublisher,
        DataProviderService dataService,List<TopoEdgeUpdate> edgeUpdates) {
        _modification = modification;
        _topologyPublisher = topologyPublisher
        _dataService = dataService
        _edgeUpdates = edgeUpdates
    }
    override finish() throws IllegalStateException {
        
        if(topologyPublisher != null && _edgeUpdates != null && !edgeUpdates.empty) {
            topologyPublisher.edgeUpdate(edgeUpdates)
        }
         
         return new RpcResultTo()
    }
    
    override getModification() {
        return _modification;
    }
    
    override rollback() throws IllegalStateException {
        // NOOP
    }
}
class RpcResultTo implements RpcResult<Void> {
    
    override getErrors() {
        return Collections.emptySet
    }
    
    override getResult() {
        return null;
    }
    
    override isSuccessful() {
        return true;
    }
    
}
