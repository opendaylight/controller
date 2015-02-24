/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.opendaylight.controller.networkconfig.neutron.INeutronObject;

import java.util.List;

/**
 * This interface defines the methods for Neutron Requests
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.northbound.api.INeutronRequest}
 */
@Deprecated
public interface INeutronRequest<T extends INeutronObject> {
    public T getSingleton();
    public boolean isSingleton();
    public List<T> getBulk();
}
