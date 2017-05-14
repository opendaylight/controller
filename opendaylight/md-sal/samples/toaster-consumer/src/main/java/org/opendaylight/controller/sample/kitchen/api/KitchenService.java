/*
 * Copyright (c) 2014, 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sample.kitchen.api;

import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.ToastType;
import org.opendaylight.yangtools.yang.common.RpcResult;

public interface KitchenService {
    Future<RpcResult<Void>> makeBreakfast(EggsType eggs, Class<? extends ToastType> toast, int toastDoneness);
}
