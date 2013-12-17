package org.opendaylight.controller.sal.restconf.impl;

import java.net.URI;

import org.opendaylight.controller.sal.rest.impl.RestUtil;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.IdentityValue;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.codec.IdentityrefCodec;
import org.opendaylight.yangtools.yang.data.api.codec.LeafrefCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestCodec {

    private RestCodec() {
    }
    
    public static final Codec<Object, Object> from(TypeDefinition<?> typeDefinition) {
        return new ObjectCodec(typeDefinition);
    }
    
    @SuppressWarnings("rawtypes")
    public static final class ObjectCodec implements Codec<Object, Object> {

        private final Logger logger = LoggerFactory.getLogger(RestCodec.class);
        
        public static final Codec IDENTITYREF_DEFAULT_CODEC = new IdentityrefCodecImpl();
        public static final Codec LEAFREF_DEFAULT_CODEC = new LeafrefCodecImpl();

        private TypeDefinition<?> type;

        private ObjectCodec(TypeDefinition<?> typeDefinition) {
            type = RestUtil.resolveBaseTypeFrom(typeDefinition);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public Object deserialize(Object input) {
            try {
                if (type instanceof IdentityrefTypeDefinition) {
                    return IDENTITYREF_DEFAULT_CODEC.deserialize(input);
                } else if (type instanceof LeafrefTypeDefinition) {
                    return LEAFREF_DEFAULT_CODEC.deserialize(input);
                } else {
                    TypeDefinitionAwareCodec<Object,? extends TypeDefinition<?>> typeAwarecodec = TypeDefinitionAwareCodec.from(type);
                    if (typeAwarecodec != null) {
                        return typeAwarecodec.deserialize(String.valueOf(input));
                    } else {
                        logger.debug("Codec for type \"" + type.getQName().getLocalName() + "\" is not implemented yet.");
                        return null;
                    }
                }
            } catch (ClassCastException e) { // TODO remove this catch when everyone use codecs
                logger.error("ClassCastException was thrown when codec is invoked with parameter " + String.valueOf(input), e);
                return input;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object serialize(Object input) {
            try {
                if (type instanceof IdentityrefTypeDefinition) {
                    return IDENTITYREF_DEFAULT_CODEC.serialize(input);
                } else if (type instanceof LeafrefTypeDefinition) {
                    return LEAFREF_DEFAULT_CODEC.serialize(input);
                } else {
                    TypeDefinitionAwareCodec<Object,? extends TypeDefinition<?>> typeAwarecodec = TypeDefinitionAwareCodec.from(type);
                    if (typeAwarecodec != null) {
                        return typeAwarecodec.serialize(input);
                    } else {
                        logger.debug("Codec for type \"" + type.getQName().getLocalName() + "\" is not implemented yet.");
                        return null;
                    }
                }
            } catch (ClassCastException e) { // TODO remove this catch when everyone use codecs
                logger.error("ClassCastException was thrown when codec is invoked with parameter " + String.valueOf(input), e);
                return input;
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
    
    public static class LeafrefCodecImpl implements LeafrefCodec<String> {

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
