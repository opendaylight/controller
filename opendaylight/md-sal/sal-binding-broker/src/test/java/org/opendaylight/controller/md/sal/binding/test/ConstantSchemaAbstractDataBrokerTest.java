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
 * AbstractDataBrokerTest which creates the SchemaContext
 * only once, and keeps it in a static, instead of re-recreating
 * it for each Test, and is thus faster.
 *
 * @author Michael Vorburger
 * @deprecated This class is no longer useful, as {@link AbstractSchemaAwareTest#getSchemaContext()} provides effective
 *             caching.
 */
@Deprecated
public class ConstantSchemaAbstractDataBrokerTest extends AbstractConcurrentDataBrokerTest {

    public ConstantSchemaAbstractDataBrokerTest() {
    }

    public ConstantSchemaAbstractDataBrokerTest(final boolean useMTDataTreeChangeListenerExecutor) {
        super(useMTDataTreeChangeListenerExecutor);
    }

    @Override
    protected SchemaContext getSchemaContext() throws Exception {
        return SchemaContextSingleton.getSchemaContext(super::getSchemaContext);
    }

}
