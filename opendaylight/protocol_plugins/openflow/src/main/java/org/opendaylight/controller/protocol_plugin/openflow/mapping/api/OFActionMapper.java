/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.protocol_plugin.openflow.mapping.api;

import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.core.Node;
import org.openflow.protocol.action.OFAction;

/**
 * Mapper / Factory mapping from {@link OFAction} to SAL objects
 * 
 * @author Tony Tkacik
 *
 * @param <T> {@link OFAction} subclass for which mapper provides
 * translation facilities.
 */
public interface OFActionMapper<T extends OFAction> {

	
	/**
	 * Returns class for which mapper provides translation
	 * facilities.
	 * 
	 * @return class for which mapper provides translation
	 * facilities.
	 */
	Class<T> getOfClass();
	
	/**
	 * Translates {@link OFAction} into {@link Action} in the
	 * context of specified {@link Node}.
	 * 
	 * Note for implemetation: 
	 * <ul>
	 *   <li>The implementations must be
	 *       thread-safe, stateless and support concurrent invocation.
	 *   <li>The invocation of method MUST NOT change state of the mapper.
	 * </ul>
	 * 
	 * @throws IllegalArgumentException
	 * @param ofAction Action to be translated
	 * @param flowNode Context flow node
	 * @return Instance of SAL {@link Action}.
	 */
	Action salFromOpenflow(OFAction ofAction, Node flowNode);
	
}

