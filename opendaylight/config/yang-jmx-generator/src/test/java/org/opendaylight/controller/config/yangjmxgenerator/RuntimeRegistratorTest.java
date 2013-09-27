/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry.Rpc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.yangtools.sal.binding.model.api.Type;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;

import com.google.common.base.Optional;

public class RuntimeRegistratorTest {
    // TODO add more tests
    protected RuntimeBeanEntry prepareRootRB(List<RuntimeBeanEntry> children) {

        DataSchemaNode dataSchemaNodeForReporting = mock(DataSchemaNode.class);
        doReturn("DataSchemaNode").when(dataSchemaNodeForReporting).toString();
        return new RuntimeBeanEntry("pa.cka.ge", dataSchemaNodeForReporting,
                "module-name", "ModuleName", true, Optional.<String> absent(),
                Collections.<AttributeIfc> emptyList(), children,
                Collections.<Rpc> emptySet());
    }

    protected RuntimeBeanEntry prepareChildRB(List<RuntimeBeanEntry> children,
            String prefix) {
        DataSchemaNode dataSchemaNodeForReporting = mock(DataSchemaNode.class);
        doReturn("DataSchemaNode").when(dataSchemaNodeForReporting).toString();
        return new RuntimeBeanEntry("pa.cka.ge", dataSchemaNodeForReporting,
                prefix + "child-name", capitalize(prefix) + "ChildName", false,
                Optional.<String> absent(),
                Collections.<AttributeIfc> emptyList(), children,
                Collections.<Rpc> emptySet());
    }

    @Test
    public void testHierarchy() {
        LeafSchemaNode leaf = mock(LeafSchemaNode.class);
        doReturn(new QName(URI.create("urn:x"), "leaf-local-name")).when(leaf)
                .getQName();
        doReturn(Collections.emptyList()).when(leaf).getUnknownSchemaNodes();
        doReturn(null).when(leaf).getDefault();
        doReturn(null).when(leaf).getDescription();

        TypeProviderWrapper typeProviderWrapper = mock(TypeProviderWrapper.class);
        Type mockedType = mock(Type.class);
        doReturn(mockedType).when(typeProviderWrapper).getType(leaf);
        doReturn("java.lang.String").when(mockedType).getFullyQualifiedName();

    }

}
