/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.registry;

import java.util.concurrent.Future;

import org.opendaylight.controller.messagebus.api.EventSource;
import org.opendaylight.controller.messagebus.api.EventSourceManager;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
/**
 * @author madamjak
 *
 */
public interface EventSourceRegistryService extends RpcService {

    Future<RpcResult<Void>> registerEventSourceManager(EventSourceManager eventSourceManager);

    Future<RpcResult<Void>> unRegisterEventSourceManager(EventSourceManager eventSourceManager);

    Future<RpcResult<EventSourceRegistration>> registerEventSource(EventSource eventSource);

    Future<RpcResult<Void>> unRegistreEventSource(EventSourceRegistration esr);

}