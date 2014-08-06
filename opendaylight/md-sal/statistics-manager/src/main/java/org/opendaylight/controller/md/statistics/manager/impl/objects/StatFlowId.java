/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl.objects;

import com.google.common.base.Preconditions;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * Planed map:
 * InstanceIdentifier.create(Nodes.class).child(Node.class,NodeKey).child(Table.class,TableKey)
 * StatFlowId
 *
 * HashCode for Flow (Match,Priority,flowCookie) from GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 27, 2014
 */
public class StatFlowId {

    private final String flowId;
    private final int deviceFlowHash;

    public StatFlowId(final String flowId, final int deviceFlowHash) {
        this.flowId = Preconditions.checkNotNull(flowId, "FlowID can not be null!");
        this.deviceFlowHash = deviceFlowHash;
    }

    @Override
    public int hashCode() {
        return deviceFlowHash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        StatFlowId other = (StatFlowId) obj;
        if (flowId == null) {
            if (other.flowId != null) {
                return false;
            }
        } else if(!flowId.equals(other.flowId)) {
            return false;
        }
        if(deviceFlowHash != (other.deviceFlowHash)) {
            return false;
        }
        return true;
    }
}
