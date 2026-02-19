/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint;

import org.osgi.framework.Bundle;

/**
 * Interface that restarts blueprint containers.
 *
 * @author Thomas Pantelis
 */
public interface BlueprintContainerRestartService {

    /**
     * Restarts the blueprint container for the given bundle and all its dependent containers in an atomic
     * and orderly manner. The dependent containers are identified by walking the OSGi service dependency
     * hierarchies for the service(s) provided by the given bundle.
     *
     * @param bundle the bundle to restart
     */
    void restartContainerAndDependents(Bundle bundle);
}
