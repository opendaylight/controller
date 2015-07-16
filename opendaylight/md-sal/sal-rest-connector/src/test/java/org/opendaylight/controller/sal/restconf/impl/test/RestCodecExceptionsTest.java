/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import java.util.Collections;
import org.junit.Test;
import org.opendaylight.controller.rest.common.RestCodec;
import org.opendaylight.controller.rest.connector.impl.RestSchemaControllerImpl;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition.Bit;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.BitsType;

public class RestCodecExceptionsTest {

    private static final SchemaPath PATH = SchemaPath.create(true, QName.create("test", "2014-05-30", "test"));
    private static final RestSchemaControllerImpl context = new RestSchemaControllerImpl();

    @Test
    public void serializeExceptionTest() {
        final Codec<Object, Object> codec = RestCodec.from(BitsType.create(PATH, Collections.<Bit> emptyList()), null,
                context);
        final String serializedValue = (String) codec.serialize("incorrect value"); // set
                                                                              // expected
        assertEquals("incorrect value", serializedValue);
    }

    @Test
    public void deserializeExceptionTest() {
        final IdentityrefTypeDefinition mockedIidentityrefType = mock(IdentityrefTypeDefinition.class);

        final Codec<Object, Object> codec = RestCodec.from(mockedIidentityrefType, null, context);
        assertNull(codec.deserialize("incorrect value"));
    }

}
