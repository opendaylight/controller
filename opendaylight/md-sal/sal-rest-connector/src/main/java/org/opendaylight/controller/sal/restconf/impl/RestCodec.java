package org.opendaylight.controller.sal.restconf.impl;

import java.net.URI;

import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.IdentityValue;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.codec.IdentityrefCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;

public class RestCodec {
    
    @SuppressWarnings("rawtypes")
    public static final Codec IDENTITYREF_DEFAULT_CODEC = new IdentityrefCodecImpl();
    
    private RestCodec() {
    }
    
    public static final Codec<Object, Object> from(TypeDefinition<?> typeDefinition) {
        return new ObjectCodec(typeDefinition);
    }
    
    public static final class ObjectCodec implements Codec<Object, Object> {

        private TypeDefinition<?> type;
        
        private ObjectCodec(TypeDefinition<?> typeDefinition) {
            type = typeDefinition;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public Object deserialize(Object input) {
            if (type instanceof IdentityrefTypeDefinition) {
                return IDENTITYREF_DEFAULT_CODEC.deserialize(input);
            } else {
                return TypeDefinitionAwareCodec.from(type).deserialize(String.valueOf(input));
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object serialize(Object input) {
            if (type instanceof IdentityrefTypeDefinition) {
                return IDENTITYREF_DEFAULT_CODEC.serialize(input);
            } else {
                return TypeDefinitionAwareCodec.from(type).serialize(input);
            }
        }
        
    }
    
    public static class IdentityrefCodecImpl implements IdentityrefCodec<IdentityValuesDTO> {

        @Override
        public IdentityValuesDTO serialize(QName data) {
            return new IdentityValuesDTO(data.getNamespace().toString(), data.getLocalName(), data.getPrefix());
        }

        @Override
        public QName deserialize(IdentityValuesDTO data) {
            IdentityValue valueWithNamespace = data.getValuesWithNamespaces().get(0);
            String namespace = valueWithNamespace.getNamespace();
            URI validNamespace = ControllerContext.getInstance().findNamespaceByModule(namespace);
            if (validNamespace == null) {
                validNamespace = URI.create(namespace);
            }
            return QName.create(validNamespace, null, valueWithNamespace.getValue());
        }

    }
    
}
