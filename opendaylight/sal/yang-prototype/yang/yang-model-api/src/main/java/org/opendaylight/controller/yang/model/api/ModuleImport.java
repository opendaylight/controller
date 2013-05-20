/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

import java.util.Date;

/**
 * Interface describing YANG 'import' statement.
 * <p>
 * The import statement makes definitions from one module available inside
 * another module or submodule.
 * </p>
 */
public interface ModuleImport {

    /**
     * @return Name of the module to import
     */
    String getModuleName();

    /**
     * @return Revision of module to import
     */
    Date getRevision();

    /**
     * @return Prefix used to point to imported module
     */
    String getPrefix();

}
