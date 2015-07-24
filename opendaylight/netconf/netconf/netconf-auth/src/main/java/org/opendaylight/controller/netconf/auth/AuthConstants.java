/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.auth;

public class AuthConstants {

    /**
     * This property should be set for every implementation of AuthService published to OSGi.
     * Netconf SSH will pick the service with highest preference in case of multiple services present in OSGi.
     */
    public static final String SERVICE_PREFERENCE_KEY = "preference";
}
