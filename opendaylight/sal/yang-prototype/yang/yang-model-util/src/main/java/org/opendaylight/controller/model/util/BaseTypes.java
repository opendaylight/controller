/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;

public final class BaseTypes {

    private BaseTypes() {
    }

    public static final URI BaseTypesNamespace = URI
            .create("urn:ietf:params:xml:ns:yang:1");

    public static final QName constructQName(final String typeName) {
        return new QName(BaseTypesNamespace, typeName);
    }

    public static final SchemaPath schemaPath(final QName typeName) {
        final List<QName> pathList = new ArrayList<QName>();
        pathList.add(typeName);
        return new SchemaPath(pathList, true);
    }
}
