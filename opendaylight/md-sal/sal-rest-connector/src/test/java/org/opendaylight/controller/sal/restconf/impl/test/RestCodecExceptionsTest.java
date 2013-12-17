package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.RestCodec;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.BitsType;

public class RestCodecExceptionsTest {

    @Test
    public void serializeExceptionTest() {
        Codec<Object, Object> codec = RestCodec.from(new BitsType(null));
        String serializedValue = (String) codec.serialize("incorrect value"); // set
                                                                              // expected
        assertEquals("incorrect value", serializedValue);
    }

    @Test
    public void deserializeExceptionTest() {
        IdentityrefTypeDefinition mockedIidentityrefType = mock(IdentityrefTypeDefinition.class);

        Codec<Object, Object> codec = RestCodec.from(mockedIidentityrefType);
        String serializedValue = (String) codec.deserialize("incorrect value"); // IdentityValuesDTO
                                                                                // object
        // expected
        assertEquals("incorrect value", serializedValue);
    }

}
