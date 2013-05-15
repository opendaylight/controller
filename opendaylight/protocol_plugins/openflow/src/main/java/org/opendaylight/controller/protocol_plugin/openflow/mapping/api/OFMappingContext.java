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
 * Context representing mapping from SAL concepts to OpenflowJ concepts
 * 
 * 
 * TODO: Extend context to provide mapping for more Openflow terms.
 * 
 * @author Tony Tkacik
 *
 */
public interface OFMappingContext {
	/**
	 * Returns a factory, which creates OpenflowJ objects
	 * from specified SAL {@link Action} type
	 * 
	 * TODO: Should throws an exception if mapping is not available?
	 * 
	 * @param salType Final class representing Action type
	 * @return Instance of factory creating {@link OFAction} instances from SAL {@link Action}
	 * for specified Action class
	 */
	<T extends Action> SALActionMapper<T> getMapperForSalAction(Class<T> salType);
	
	/**
	 * Returns a factory, which creating SAL objects from specified {@link OFAction} type
	 * 
	 * TODO: Should throws an exception if mapping is not available?
	 * 
	 * @param ofType Final class representing Action type
	 * @return Instance of factory creating {@link OFAction} instances from SAL {@link Action}
	 * for specified Action class
	 */
	<T extends OFAction> OFActionMapper<T> getMapperForOFAction(Class<T> ofType);
	
}
