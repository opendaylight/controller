/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology;

import io.netty.util.concurrent.EventExecutor;
import org.opendaylight.controller.config.threadpool.ScheduledThreadPool;
import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.api.Broker;

public interface NetconfTopology {

    NetconfClientDispatcher getNetconfClientDispatcherDependency();

    BindingAwareBroker getBindingAwareBroker();

    Broker getDomRegistryDependency();

    EventExecutor getEventExecutorDependency();

    ScheduledThreadPool getKeepaliveExecutorDependency();

    ThreadPool getProcessingExecutorDependency();

    SchemaRepositoryProvider getSharedSchemaRepository();

}
