/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.service.impl;

import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.BeginTransactionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.CreateDataChangeEventSubscriptionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.CreateDataChangeEventSubscriptionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.CreateNotificationStreamInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.CreateNotificationStreamOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.SalRemoteService;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class SalRemoteServiceImpl implements SalRemoteService {
    @Override
    public Future<RpcResult<BeginTransactionOutput>> beginTransaction() {
        return null;
    }

    @Override
    public Future<RpcResult<CreateDataChangeEventSubscriptionOutput>> createDataChangeEventSubscription(CreateDataChangeEventSubscriptionInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<CreateNotificationStreamOutput>> createNotificationStream(CreateNotificationStreamInput input) {
        return null;
    }
}
