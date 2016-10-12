/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.notifications;

import java.io.Serializable;

/**
 * Message sent from the listener of Role Change messages to register itself to the Role Change Notifier.
 * The Listener could be in a separate ActorSystem and hence this message needs to be Serializable.
 */
public class RegisterRoleChangeListener implements Serializable {
    private static final long serialVersionUID = 8370459011119791506L;
}
