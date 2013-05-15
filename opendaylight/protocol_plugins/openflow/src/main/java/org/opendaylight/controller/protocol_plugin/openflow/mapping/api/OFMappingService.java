/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.protocol_plugin.openflow.mapping.api;

import org.opendaylight.controller.sal.action.Action;
import org.openflow.protocol.action.OFAction;

/**
 * 
 * 
 * 
 * 
 * @author Tony Tkacik
 * 
 */
public interface OFMappingService {

    public OFMappingContext getMappingContext();

    /**
     * Register a mapper / factory which maps SAL Actions to OpenflowJ objects.
     * 
     * 
     * Only one mapper for specified class could be registered.
     * 
     * @param mapper
     *            Mapper to be registered.
     * 
     * @throws IllegalArgumentException
     *             If the mapper is null
     * @throws IllegalStateException
     *             If the mapper for specified action is already registered.
     * 
     */
    public void addSALActionMapper(SALActionMapper<? extends Action> mapper);

    /**
     * Register a mapper / factory which maps OpenflowJ Actions to SAL objects.
     * 
     * Only one mapper for specified class could be registered.
     * 
     * @param mapper
     * @throws IllegalArgumentException
     *             If the mapper is null
     * @throws IllegalStateException
     *             If the mapper for specified action is already registered.
     */
    public void addOFActionMapper(OFActionMapper<? extends OFAction> mapper);
}
