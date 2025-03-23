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
 * Reply message sent from a RoleChangeNotifier to the Role Change Listener.
 * Can be sent to a separate actor system and hence should be made serializable.
 */
// FIXME: get a cookie or something?
// FIXME: definitely final
public class RegisterRoleChangeListenerReply implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = -1972061601184451430L;
}
