
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   CacheExistException.java
 *
 * @brief  Describe an exception that is raised when the cache being
 * allocated already exists
 *
 *
 */
package org.opendaylight.controller.clustering.services;

import java.lang.Exception;

/**
 * Describe an exception that is raised when the cache being
 * allocated already exists
 */
public class CacheExistException extends Exception {

    /**
     * Instantiates a new cache exist exception.
     */
    public CacheExistException() {
        super();
    }
}
