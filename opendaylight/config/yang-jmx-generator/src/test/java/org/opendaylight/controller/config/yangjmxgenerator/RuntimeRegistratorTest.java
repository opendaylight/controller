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
import com.google.common.base.Optional;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry.Rpc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.mdsal.binding.model.api.Type;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;

public class RuntimeRegistratorTest {
    // TODO add more tests
    protected RuntimeBeanEntry prepareRootRB(final List<RuntimeBeanEntry> children) {

        final DataNodeContainer nodeContainer = mock(DataNodeContainer.class);
        doReturn("DataSchemaNode").when(nodeContainer).toString();
        return new RuntimeBeanEntry("pa.cka.ge", nodeContainer,
                "module-name", "ModuleName", true, Optional.<String> absent(),
                Collections.<AttributeIfc> emptyList(), children,
                Collections.<Rpc> emptySet());
    }

    protected RuntimeBeanEntry prepareChildRB(final List<RuntimeBeanEntry> children,
            final String prefix) {
        final DataNodeContainer nodeContainer = mock(DataNodeContainer.class);
        doReturn("DataSchemaNode").when(nodeContainer).toString();
        return new RuntimeBeanEntry("pa.cka.ge", nodeContainer,
                prefix + "child-name", capitalize(prefix) + "ChildName", false,
                Optional.<String> absent(),
                Collections.<AttributeIfc> emptyList(), children,
                Collections.<Rpc> emptySet());
    }

    @Test
    public void testHierarchy() {
        final LeafSchemaNode leaf = mock(LeafSchemaNode.class);
        doReturn(new QName(URI.create("urn:x"), "leaf-local-name")).when(leaf)
                .getQName();
        doReturn(Collections.emptyList()).when(leaf).getUnknownSchemaNodes();
        doReturn(null).when(leaf).getDefault();
        doReturn(null).when(leaf).getDescription();

        final TypeProviderWrapper typeProviderWrapper = mock(TypeProviderWrapper.class);
        final Type mockedType = mock(Type.class);
        doReturn(mockedType).when(typeProviderWrapper).getType(leaf);
        doReturn("java.lang.String").when(mockedType).getFullyQualifiedName();

    }

}
