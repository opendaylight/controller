/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.networkconfig.bridgedomain;

/**
 * Exception thrown by IPluginInBridgeDomainConfigService implementations.
 */
public class BridgeDomainConfigServiceException extends Exception {
    private static final long serialVersionUID = 1L;

    public BridgeDomainConfigServiceException(String message) {
        super(message);
    }

    public BridgeDomainConfigServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

