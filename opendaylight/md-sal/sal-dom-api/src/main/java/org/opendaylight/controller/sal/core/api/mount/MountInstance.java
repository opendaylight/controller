/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.sal.core.api.mount;

import java.util.concurrent.Future;

import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.notify.NotificationService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public interface MountInstance extends NotificationService, DataBrokerService {

    Future<RpcResult<CompositeNode>> rpc(QName type, CompositeNode input);
}
