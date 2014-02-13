/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.queue.rev130925.QueueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;

final class QueueStatsEntry {
    private final NodeConnectorId nodeConnectorId;
    private final QueueId queueId;
    public QueueStatsEntry(NodeConnectorId ncId, QueueId queueId){
        this.nodeConnectorId = ncId;
        this.queueId = queueId;
    }
    public NodeConnectorId getNodeConnectorId() {
        return nodeConnectorId;
    }
    public QueueId getQueueId() {
        return queueId;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nodeConnectorId == null) ? 0 : nodeConnectorId.hashCode());
        result = prime * result + ((queueId == null) ? 0 : queueId.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof QueueStatsEntry)) {
            return false;
        }
        QueueStatsEntry other = (QueueStatsEntry) obj;
        if (nodeConnectorId == null) {
            if (other.nodeConnectorId != null) {
                return false;
            }
        } else if (!nodeConnectorId.equals(other.nodeConnectorId)) {
            return false;
        }
        if (queueId == null) {
            if (other.queueId != null) {
                return false;
            }
        } else if (!queueId.equals(other.queueId)) {
            return false;
        }
        return true;
    }
}
