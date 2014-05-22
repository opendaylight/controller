/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl;

import java.util.Map;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ServiceReferenceWritableRegistry;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;

public interface SearchableServiceReferenceWritableRegistry extends ServiceReferenceWritableRegistry {
    /**
     * Return mapping between service ref names and service interface annotation for given
     * module.
     *
     * @throws java.lang.IllegalArgumentException if any of service qNames is not found
     * @throws java.lang.NullPointerException     if parameter is null
     */
    Map<ServiceInterfaceAnnotation, String /* service ref name */> findServiceInterfaces(ModuleIdentifier moduleIdentifier);

}
