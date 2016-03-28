/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api.osgi;

/**
 * RuntimeException thrown when an OSGi service lookup fails.
 *
 * @author Thomas Pantelis
 */
public class ServiceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceNotFoundException(String message) {
        super(message);
    }
}
