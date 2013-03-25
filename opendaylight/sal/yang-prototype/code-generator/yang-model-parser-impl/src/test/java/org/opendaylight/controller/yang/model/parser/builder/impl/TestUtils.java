/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;

public class TestUtils {

    private TestUtils() {
    }

    public static SchemaPath createSchemaPath(boolean absolute, URI namespace,
            String... path) {
        List<QName> names = new ArrayList<QName>();
        QName qname;
        for (String pathPart : path) {
            qname = new QName(namespace, pathPart);
            names.add(qname);
        }
        return new SchemaPath(names, absolute);
    }

}
