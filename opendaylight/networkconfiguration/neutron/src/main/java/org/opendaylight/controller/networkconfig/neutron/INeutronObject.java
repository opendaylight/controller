/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Authors : Dave Tucker
 */

package org.opendaylight.controller.networkconfig.neutron;

/**
 * This class contains behaviour common to Neutron configuration objects
 *
 * @deprecated Replaced by {@link org.opendaylight.neutron.neutron.spi.INeutronObject}
 */
@Deprecated
public interface INeutronObject {
    public String getID();
    public void setID(String id);
}
