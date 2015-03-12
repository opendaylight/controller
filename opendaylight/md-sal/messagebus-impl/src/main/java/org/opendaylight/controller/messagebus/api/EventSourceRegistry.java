/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.api;

import java.util.concurrent.Future;

import org.opendaylight.controller.messagebus.registry.EventSourceRegistration;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
/**
 * @author madamjak
 *
 */
public interface EventSourceRegistry extends RpcService {

    Future<RpcResult<EventSourceRegistration>> registerEventSource(final Node sourceNode, final EventSource eventSource);

    Future<RpcResult<Void>> unRegistreEventSource(EventSourceRegistration esr);

}