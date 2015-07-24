/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mapping.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public final class HandlingPriority implements Comparable<HandlingPriority> {

    public static final HandlingPriority CANNOT_HANDLE = new HandlingPriority();
    public static final HandlingPriority HANDLE_WITH_DEFAULT_PRIORITY = new HandlingPriority(Integer.MIN_VALUE);
    public static final HandlingPriority HANDLE_WITH_MAX_PRIORITY = new HandlingPriority(Integer.MAX_VALUE);

    private Integer priority;

    public static HandlingPriority getHandlingPriority(int priority) {
        return new HandlingPriority(priority);
    }

    private HandlingPriority(int priority) {
        this.priority = priority;
    }

    private HandlingPriority() {
    }

    /**
     * @return priority number or Optional.absent otherwise
     */
    public Optional<Integer> getPriority() {
        return Optional.fromNullable(priority);
    }

    public HandlingPriority increasePriority(int priorityIncrease) {
        Preconditions.checkState(priority!=null, "Unable to increase priority for %s", this);
        Preconditions.checkArgument(priorityIncrease > 0, "Negative increase");
        Preconditions.checkArgument(Long.valueOf(priority) + priorityIncrease < Integer.MAX_VALUE,
                "Resulting priority cannot be higher than %s", Integer.MAX_VALUE);
        return getHandlingPriority(priority + priorityIncrease);
    }

    public boolean isCannotHandle() {
        return this.equals(CANNOT_HANDLE);
    }

    @Override
    public int compareTo(HandlingPriority o) {
        if (this == o) {
            return 0;
        }
        if (isCannotHandle()) {
            return -1;
        }
        if (o.isCannotHandle()) {
            return 1;
        }

        if (priority > o.priority){
            return 1;
        }
        if (priority.equals(o.priority)){
            return 0;
        }
        if (priority < o.priority){
            return -1;
        }

        throw new IllegalStateException("Unexpected state comparing " + this + " with " + o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (!(o instanceof HandlingPriority)){
            return false;
        }

        HandlingPriority that = (HandlingPriority) o;

        if (priority != null ? !priority.equals(that.priority) : that.priority != null){
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return priority != null ? priority.hashCode() : 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("priority", priority)
                .toString();
    }
}
