package org.opendaylight.controller.md.sal.rest.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

public class RestconfContext {

    private static final LoadingCache<SchemaContext, RestconfContext> INSTANCES = CacheBuilder
            .newBuilder()
            .weakKeys()
            .build(new CacheLoader<SchemaContext, RestconfContext>() {

                @Override
                public RestconfContext load(final SchemaContext key) throws Exception {
                    return new RestconfContext(key);
                }
            });

    private static final XmlCodecProvider DEFAULT_XML_CODEC_PROVIDER = new XmlCodecProvider() {
        @Override
        public TypeDefinitionAwareCodec<Object, ? extends TypeDefinition<?>> codecFor(final TypeDefinition<?> baseType) {
            return TypeDefinitionAwareCodec.from(baseType);
        }
    };

    private final SchemaContext context;
    private final JSONCodecFactory jsonCodecs;
    private final DomToNormalizedNodeParserFactory domCodecs;


    private RestconfContext(final SchemaContext context) {
        this.context = context;
        jsonCodecs = JSONCodecFactory.create(context);
        domCodecs = DomToNormalizedNodeParserFactory.getInstance(DEFAULT_XML_CODEC_PROVIDER, context);
    }

    public static final RestconfContext from(final SchemaContext ctx) {
        return INSTANCES.getUnchecked(ctx);
    }


    public SchemaContext getContext() {
        return context;
    }

    public DomToNormalizedNodeParserFactory getDomCodecFactory() {
        return domCodecs;
    }

    public JSONCodecFactory getJsonCodecFactory() {
        return jsonCodecs;
    }
}
