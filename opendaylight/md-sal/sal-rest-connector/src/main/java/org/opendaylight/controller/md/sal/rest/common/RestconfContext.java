package org.opendaylight.controller.md.sal.rest.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

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

    private final SchemaContext context;
    private final JSONCodecFactory jsonCodecs;


    private RestconfContext(final SchemaContext context) {
        this.context = context;
        this.jsonCodecs = JSONCodecFactory.create(context);
    }

    public static final RestconfContext from(final SchemaContext ctx) {
        return INSTANCES.getUnchecked(ctx);
    }


    public SchemaContext getContext() {
        return context;
    }

    public JSONCodecFactory getJsonCodecFactory() {
        return jsonCodecs;
    }
}
