/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;

final class FlowStatsEntry {
    private final Short tableId;
    private final Flow flow;

    public FlowStatsEntry(Short tableId, Flow flow){
        this.tableId = tableId;
        this.flow = flow;
    }

    public Short getTableId() {
        return tableId;
    }

    public Flow getFlow() {
        return flow;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((flow == null) ? 0 : flow.hashCode());
        result = prime * result + ((tableId == null) ? 0 : tableId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FlowStatsEntry other = (FlowStatsEntry) obj;
        if (flow == null) {
            if (other.flow != null)
                return false;
        } else if (!flow.equals(other.flow))
            return false;
        if (tableId == null) {
            if (other.tableId != null)
                return false;
        } else if (!tableId.equals(other.tableId))
            return false;
        return true;
    }
}
