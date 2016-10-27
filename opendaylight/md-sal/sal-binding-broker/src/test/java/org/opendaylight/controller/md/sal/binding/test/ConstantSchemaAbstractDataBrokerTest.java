/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Faster AbstractDataBrokerTest.
 * Suitable for constant YANG schema on the classpath, which is all typical such tests.
 * The AbstractDataBrokerTest re-creates a SchemaContext for each Test method, which is
 * a time consuming operation; this does it only once for all *Test classes, and then
 * keeps it shared in a static.
 *
 * @author Michael Vorburger
 */
public class ConstantSchemaAbstractDataBrokerTest extends AbstractDataBrokerTest {

    @Override
    protected SchemaContext getSchemaContext() throws Exception {
        return SchemaContextSingleton.getSchemaContext(() -> super.getSchemaContext());
    }

}
