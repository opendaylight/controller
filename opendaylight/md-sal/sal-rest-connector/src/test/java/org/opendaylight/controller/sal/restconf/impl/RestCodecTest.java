/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.util.Iterator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.IdentityValue;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.type.InstanceIdentifierTypeDefinition;

public class RestCodecTest {

    private static InstanceIdentifierTypeDefinition mockedTypeInstanceIdentifier;
    private static DOMMountPoint mockedMountPoint;

    @BeforeClass
    public static void initialize() throws FileNotFoundException {
        mockedTypeInstanceIdentifier = mock(InstanceIdentifierTypeDefinition.class);

        SchemaContext schemaContext = TestUtils.loadSchemaContext("/full-versions/test-module");
        mockedMountPoint = mock(DOMMountPoint.class);
        when(mockedMountPoint.getSchemaContext()).thenReturn(schemaContext);
    }

    /**
     * Test deserialization of instance identifier when data are behind mount point (mount point is specified)
     *
     * @throws FileNotFoundException
     */
    @Test
    public void deserializeInstanceIdentifierTest() {
        Codec<Object, Object> codec = RestCodec.from(mockedTypeInstanceIdentifier, mockedMountPoint);

        IdentityValuesDTO input = new IdentityValuesDTO();
        IdentityValue identityValue1 = new IdentityValue("test:module", "cont", "pref");
        IdentityValue identityValue2 = new IdentityValue("test:module", "cont1", "pref");

        input.add(identityValue1);
        input.add(identityValue2);

        Object deserialized = codec.deserialize(input);
        assertTrue(deserialized instanceof YangInstanceIdentifier);
        YangInstanceIdentifier instanceIdentifier = (YangInstanceIdentifier) deserialized;
        Iterator<PathArgument> pathArguments = instanceIdentifier.getPathArguments().iterator();

        PathArgument pathArgument = null;
        assertTrue(pathArguments.hasNext());
        pathArgument = pathArguments.next();
        assertEquals(QName.create("test:module", "2014-01-09", "cont"), pathArgument.getNodeType());

        assertTrue(pathArguments.hasNext());
        pathArgument = pathArguments.next();
        assertEquals(QName.create("test:module", "2014-01-09", "cont1"), pathArgument.getNodeType());

        assertFalse(pathArguments.hasNext());
    }

    @Test
    public void deserializeInstanceIdentifierWithMissingModuleNameTest() {
        Codec<Object, Object> codec = RestCodec.from(mockedTypeInstanceIdentifier, mockedMountPoint);

        IdentityValuesDTO input = new IdentityValuesDTO();
        IdentityValue identityValue1 = new IdentityValue("invalid:test:module", "cont", "pref");
        IdentityValue identityValue2 = new IdentityValue("invalid:test:module", "cont1", "pref");

        input.add(identityValue1);
        input.add(identityValue2);

        Object deserialized = codec.deserialize(input);

        assertNull(deserialized);
    }

}
