/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mapping.api;

import com.google.common.base.Optional;

public class HandlingPriority implements Comparable<HandlingPriority> {

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
        return Optional.of(priority).or(Optional.<Integer> absent());
    }

    // TODO test

    @Override
    public int compareTo(HandlingPriority o) {
        if (this == o)
            return 0;
        if (this == CANNOT_HANDLE)
            return -1;
        if (o == CANNOT_HANDLE)
            return 1;

        if (priority > o.priority)
            return 1;
        if (priority == o.priority)
            return 0;
        if (priority < o.priority)
            return -1;

        throw new IllegalStateException("Unexpected state");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof HandlingPriority))
            return false;

        HandlingPriority that = (HandlingPriority) o;

        if (priority != null ? !priority.equals(that.priority) : that.priority != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return priority != null ? priority.hashCode() : 0;
    }
}
