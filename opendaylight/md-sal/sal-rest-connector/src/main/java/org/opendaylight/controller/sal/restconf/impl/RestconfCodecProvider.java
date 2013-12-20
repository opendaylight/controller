package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.codec.LeafrefCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.Leafref;

import com.google.common.base.Optional;

public class RestconfCodecProvider implements XmlCodecProvider {

    @Override
    public TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> codecFor(TypeDefinition<?> baseType) {
        TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> codec = TypeDefinitionAwareCodec.from(baseType);

        if (codec == null) {
            if (baseType instanceof Leafref) {
                return new LeafrefCodecImpl(Optional.<LeafrefTypeDefinition> absent());
            }
        }
        return codec;
    }

    private class LeafrefCodecImpl extends TypeDefinitionAwareCodec<Object, LeafrefTypeDefinition> implements
            LeafrefCodec<String> {

        protected LeafrefCodecImpl(Optional<LeafrefTypeDefinition> typeDef) {
            super(typeDef, Object.class);
        }

        @Override
        public String serialize(Object data) {
            return String.valueOf(data);
        }

        @Override
        public Object deserialize(String data) {
            return data;
        }

    }

}
