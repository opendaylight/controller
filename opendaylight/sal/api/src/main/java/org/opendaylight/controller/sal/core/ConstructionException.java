
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   ConstructionException.java
 *
 *
 * @brief  Describe an exception that is raised when a construction
 * for a Node/NodeConnector/Edge or any of the SAL basic object fails
 * because input passed are not valid or compatible
 *
 *
 */
package org.opendaylight.controller.sal.core;

import java.lang.Exception;

public class ConstructionException extends Exception {
    private static final long serialVersionUID = 1L;

    public ConstructionException(String message) {
        super(message);
    }
}
