/*
 * Copyright (c) 2014 Ciena Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

/**
 * Marker interface for class routing policy implementations. An implementation of this interface defines how
 * notifications are routed when multiple instances of the class have registered for the same notification. For example
 * if withinn a OpenDaylight (ODL) cluster two nodes are running module X, and each instance of the module registers
 * for notification Y, when a notifcation of Y is published the policy is used to determine which instances of module X
 * recieve the notification.
 *
 * Note, two instances are defined as registered for the same notification if (a) the class routing policy they were
 * registered against are the same as determined by the "equals" method and (b) if the listener filter specified
 * during the registration match the notification, i.e. in other words if the filters are such that both would
 * received the notification if there were not class routing policy.
 */
public interface ClassRoutingPolicy {

}