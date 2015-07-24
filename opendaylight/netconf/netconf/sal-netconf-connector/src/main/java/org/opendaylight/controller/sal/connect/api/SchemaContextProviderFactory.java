/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.api;

import java.io.InputStream;
import java.util.Collection;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;

public interface SchemaContextProviderFactory {

    SchemaContextProvider createContextProvider(Collection<QName> capabilities, SchemaSourceProvider<InputStream> sourceProvider);

}