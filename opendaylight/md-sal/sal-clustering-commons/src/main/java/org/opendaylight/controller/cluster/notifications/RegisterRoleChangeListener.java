/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.notifications;

/**
 * Message sent from the listener of Role Change messages to register itself to the Role Change Notifier
 *
 * Currently these  are sent within the same node, hence are not Serializable
 */
public class RegisterRoleChangeListener {
}
