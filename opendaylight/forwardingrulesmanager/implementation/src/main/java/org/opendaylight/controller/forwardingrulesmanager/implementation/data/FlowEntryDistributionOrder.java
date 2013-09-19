/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Class used by the FRM to distribute the forwarding rules programming in the
 * cluster and to collect back the results of the programming
 */
package org.opendaylight.controller.forwardingrulesmanager.implementation.data;

import java.io.Serializable;
import java.net.InetAddress;

import org.opendaylight.controller.forwardingrulesmanager.FlowEntryInstall;
import org.opendaylight.controller.sal.core.UpdateType;

/**
 * Class used by the FRM to distribute the forwarding rules programming in the
 * cluster and to collect back the results of the programming
 */
public final class FlowEntryDistributionOrder implements Serializable {
    /**
     * Serialization UID
     */
    private static final long serialVersionUID = 416280377113255147L;
    private final FlowEntryInstall entry;
    private final UpdateType upType;
    private final InetAddress requestorController;

    /**
     * @return the entry
     */
    public FlowEntryInstall getEntry() {
        return entry;
    }

    /**
     * @return the upType
     */
    public UpdateType getUpType() {
        return upType;
    }

    /**
     * @return the requestorController
     */
    public InetAddress getRequestorController() {
        return requestorController;
    }

    /**
     * @param entry
     *            FlowEntryInstall key value
     * @param upType
     *            UpdateType key value
     * @param requestorController
     *            identifier of the controller that initiated the request
     */

    public FlowEntryDistributionOrder(FlowEntryInstall entry, UpdateType upType, InetAddress requestorController) {
        super();
        this.entry = entry;
        this.upType = upType;
        this.requestorController = requestorController;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((entry == null) ? 0 : entry.hashCode());
        result = (prime * result) + ((requestorController == null) ? 0 : requestorController.hashCode());
        result = (prime * result) + ((upType == null) ? 0 : upType.calculateConsistentHashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof FlowEntryDistributionOrder)) {
            return false;
        }
        FlowEntryDistributionOrder other = (FlowEntryDistributionOrder) obj;
        if (entry == null) {
            if (other.entry != null) {
                return false;
            }
        } else if (!entry.equals(other.entry)) {
            return false;
        }
        if (requestorController == null) {
            if (other.requestorController != null) {
                return false;
            }
        } else if (!requestorController.equals(other.requestorController)) {
            return false;
        }
        if (upType != other.upType) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FlowEntryDistributionOrder [");
        if (entry != null) {
            builder.append("entry=")
                    .append(entry)
                    .append(", ");
        }
        if (upType != null) {
            builder.append("upType=")
                    .append(upType)
                    .append(", ");
        }
        if (requestorController != null) {
            builder.append("requestorController=")
                    .append(requestorController);
        }
        builder.append("]");
        return builder.toString();
    }
}
