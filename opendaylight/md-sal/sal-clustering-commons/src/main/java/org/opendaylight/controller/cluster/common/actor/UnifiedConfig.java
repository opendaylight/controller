/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import com.typesafe.config.Config;

/**
 * Represents a unified view of configuration.
 *
 * <p>
 * It merges configuration from:
 * <ul>
 *     <li>Config subsystem</li>
 *     <li>Akka configuration files</li>
 * </ul>
 * Configurations defined in config subsystem takes precedence.
 */
public interface UnifiedConfig {

    /**
     * Returns an immutable instance of unified configuration.
     *
     * @return a Config instance
     */
    Config get();
}
