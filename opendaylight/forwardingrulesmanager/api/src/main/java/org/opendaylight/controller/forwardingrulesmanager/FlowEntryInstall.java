/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager;

import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.Node;

/**
 * The flow database object representing the flow entry to install on the
 * network node. It contains the original flow entry FRM was requested to
 * install, the container flow with which that entry had to be merged and the
 * resultant merged flow entry, which is the one that was eventually installed
 * on the network node
 *
 * Note: If the container flow is null, the install entry will be a clone of the
 * original entry
 *
 */
public class FlowEntryInstall {
    private FlowEntry original;
    private ContainerFlow cFlow;
    private FlowEntry install;
    transient private long requestId; // async request id
    transient private boolean deletePending;

    public FlowEntryInstall(FlowEntry original, ContainerFlow cFlow) {
        this.original = original;
        this.cFlow = cFlow;
        this.install = (cFlow == null) ? original.clone() : original
                .mergeWith(cFlow);
        deletePending = false;
        requestId = 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cFlow == null) ? 0 : cFlow.hashCode());
        result = prime * result + ((install == null) ? 0 : install.hashCode());
        result = prime * result
                + ((original == null) ? 0 : original.hashCode());
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
        FlowEntryInstall other = (FlowEntryInstall) obj;
        if (cFlow == null) {
            if (other.cFlow != null)
                return false;
        } else if (!cFlow.equals(other.cFlow))
            return false;
        if (install == null) {
            if (other.install != null)
                return false;
        } else if (!install.equals(other.install))
            return false;
        if (original == null) {
            if (other.original != null)
                return false;
        } else if (!original.equals(other.original))
            return false;
        return true;
    }

    public String getFlowName() {
        return original.getFlowName();
    }

    public String getGroupName() {
        return original.getGroupName();
    }

    public Node getNode() {
        return original.getNode();
    }

    public boolean equalsByNodeAndName(Node node, String flowName) {
        return original.equalsByNodeAndName(node, flowName);
    }

    public FlowEntry getOriginal() {
        return original;
    }

    public ContainerFlow getContainerFlow() {
        return cFlow;
    }

    public FlowEntry getInstall() {
        return install;
    }

    public boolean isDeletePending() {
        return deletePending;
    }

    public void toBeDeleted() {
        this.deletePending = true;
    }

    public void setRequestId(long rid) {
        this.requestId = rid;
    }

    public long getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        return "[Install = " + install + " Original = " + original + " cFlow = "
                + cFlow + " rid = " + requestId + "]";
    }
}
