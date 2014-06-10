/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.api;

import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;

public interface SchemaSourceProviderFactory<T> {

    SchemaSourceProvider<T> createSourceProvider(final RpcImplementation deviceRpc);
}
