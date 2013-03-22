
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   CacheConfigException.java
 *
 * @brief  Describe an exception that is raised when the cache being
 * allocated has configuration errors, like mismatch parameters are
 * passed and so on.
 *
 *
 */
package org.opendaylight.controller.clustering.services;

import java.lang.Exception;

/**
 * Describe an exception that is raised when the cache being
 * allocated has configuration errors, like mismatch parameters are
 * passed and so on.
 */
public class CacheConfigException extends Exception {

    /**
     * Instantiates a new cache config exception.
     */
    public CacheConfigException() {
        super();
    }
}
