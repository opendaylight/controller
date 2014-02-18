/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.routing;

import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class RoutingUtils {

    public static <C,P> RouteChange<C,P> removalChange(C context,P path) {
        final ImmutableMap<C, Set<P>> announcements = ImmutableMap.<C,Set<P>>of();
        final ImmutableMap<C, Set<P>> removals = ImmutableMap.<C,Set<P>>of(context, ImmutableSet.of(path));
        return new RouteChangeImpl<C,P>(announcements, removals);
    }

    public static <C,P> RouteChange<C,P> announcementChange(C context,P path) {
        final ImmutableMap<C, Set<P>> announcements = ImmutableMap.<C,Set<P>>of(context, ImmutableSet.of(path));
        final ImmutableMap<C, Set<P>> removals = ImmutableMap.<C,Set<P>>of();
        return new RouteChangeImpl<C,P>(announcements, removals);
    }


    public static <C,P> RouteChange<C,P> change(Map<C, Set<P>> announcements,
            Map<C, Set<P>> removals) {
        final ImmutableMap<C, Set<P>> immutableAnnouncements = ImmutableMap.<C,Set<P>>copyOf(announcements);
        final ImmutableMap<C, Set<P>> immutableRemovals = ImmutableMap.<C,Set<P>>copyOf(removals);
        return new RouteChangeImpl<C,P>(immutableAnnouncements, immutableRemovals);
    }


    private static class RouteChangeImpl<C,P> implements RouteChange<C, P> {
        private final Map<C, Set<P>> removal;
        private final Map<C, Set<P>> announcement;

        public RouteChangeImpl(ImmutableMap<C, Set<P>> announcement, ImmutableMap<C, Set<P>> removal) {
            super();
            this.removal = removal;
            this.announcement = announcement;
        }

        @Override
        public Map<C, Set<P>> getAnnouncements() {
            return announcement;
        }

        @Override
        public Map<C, Set<P>> getRemovals() {
            return removal;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((announcement == null) ? 0 : announcement.hashCode());
            result = prime * result + ((removal == null) ? 0 : removal.hashCode());
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
            if (getClass() != obj.getClass()) {
                return false;
            }
            RouteChangeImpl<?, ?> other = (RouteChangeImpl<?, ?>) obj;
            if (announcement == null) {
                if (other.announcement != null)
                    return false;
            } else if (!announcement.equals(other.announcement))
                return false;
            if (removal == null) {
                if (other.removal != null) {
                    return false;
                }
            } else if (!removal.equals(other.removal))
                return false;
            return true;
        }
    }



}
