/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

/**
 * Service interface for the config system.
 *
 * @author Thomas Pantelis
 */
public interface ConfigSystemService {
    /**
     * This method closes all the config system modules. This method should only be called on process
     * shutdown and is provided as a hook to control the shutdown sequence.
     */
    void closeAllConfigModules();
}
