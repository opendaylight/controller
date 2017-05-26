/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.api;

/**
 * Exception thrown when a ModuleFactory is not found while pushing a config.
 *
 * @author Thomas Pantelis
 */
public class ModuleFactoryNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String moduleName;

    public ModuleFactoryNotFoundException(String moduleName) {
        super("ModuleFactory not found for module name: " + moduleName);
        this.moduleName = moduleName;
    }

    public String getModuleName() {
        return moduleName;
    }
}
