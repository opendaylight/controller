/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.toaster.provider.api;

import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.ToastType;

public interface ToastConsumer {
	
	boolean createToast(Class<? extends ToastType> type,int doneness);

}
