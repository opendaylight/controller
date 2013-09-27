/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api.jmx;

import java.beans.ConstructorProperties;
import java.util.Collections;
import java.util.List;

import javax.annotation.concurrent.Immutable;
import javax.management.ObjectName;

@Immutable
public class CommitStatus {
    private final List<ObjectName> newInstances, reusedInstances,
            recreatedInstances;

    /**
     *
     * @param newInstances
     *            newly created instances
     * @param reusedInstances
     *            reused instances
     * @param recreatedInstances
     *            recreated instances
     */
    @ConstructorProperties({ "newInstances", "reusedInstances",
            "recreatedInstances" })
    public CommitStatus(List<ObjectName> newInstances,
            List<ObjectName> reusedInstances,
            List<ObjectName> recreatedInstances) {
        this.newInstances = Collections.unmodifiableList(newInstances);
        this.reusedInstances = Collections.unmodifiableList(reusedInstances);
        this.recreatedInstances = Collections
                .unmodifiableList(recreatedInstances);
    }

    /**
     *
     * @return list of objectNames representing newly created instances
     */
    public List<ObjectName> getNewInstances() {
        return newInstances;
    }

    /**
     *
     * @return list of objectNames representing reused instances
     */
    public List<ObjectName> getReusedInstances() {
        return reusedInstances;
    }

    /**
     *
     * @return list of objectNames representing recreated instances
     */
    public List<ObjectName> getRecreatedInstances() {
        return recreatedInstances;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((newInstances == null) ? 0 : newInstances.hashCode());
        result = prime
                * result
                + ((recreatedInstances == null) ? 0 : recreatedInstances
                        .hashCode());
        result = prime * result
                + ((reusedInstances == null) ? 0 : reusedInstances.hashCode());
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
        CommitStatus other = (CommitStatus) obj;
        if (newInstances == null) {
            if (other.newInstances != null)
                return false;
        } else if (!newInstances.equals(other.newInstances))
            return false;
        if (recreatedInstances == null) {
            if (other.recreatedInstances != null)
                return false;
        } else if (!recreatedInstances.equals(other.recreatedInstances))
            return false;
        if (reusedInstances == null) {
            if (other.reusedInstances != null)
                return false;
        } else if (!reusedInstances.equals(other.reusedInstances))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CommitStatus [newInstances=" + newInstances
                + ", reusedInstances=" + reusedInstances
                + ", recreatedInstances=" + recreatedInstances + "]";
    }

}
