package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class BindingDOMRpcProviderServiceAdapter {

    private static final Set<YangInstanceIdentifier> GLOBAL = ImmutableSet.of(YangInstanceIdentifier.builder().build());
    private final BindingToNormalizedNodeCodec codec;
    private final DOMRpcProviderService domRpcRegistry;

    public BindingDOMRpcProviderServiceAdapter(final DOMRpcProviderService domRpcRegistry, final BindingToNormalizedNodeCodec codec) {
        this.codec = codec;
        this.domRpcRegistry = domRpcRegistry;
    }

    public <S extends RpcService, T extends S> ObjectRegistration<T> registerRpcImplementation(final Class<S> type,
            final T implementation) {
        return register(type,implementation,createDomRpcIdentifiers(type,GLOBAL));
    }

    public <S extends RpcService, T extends S> ObjectRegistration<T> registerRpcImplementation(final Class<S> type,
            final T implementation, final Set<InstanceIdentifier<?>> paths) {
        return register(type,implementation,createDomRpcIdentifiers(type,toYangInstanceIdentifiers(paths)));
    }

    private <S extends RpcService, T extends S> ObjectRegistration<T> register(final Class<S> type, final T implementation, final Set<DOMRpcIdentifier> domRpcs) {
        final BindingRpcImplementationAdapter adapter = new BindingRpcImplementationAdapter(codec.getCodecFactory(), type, implementation);


        final DOMRpcImplementationRegistration<?> domReg = domRpcRegistry.registerRpcImplementation(adapter, domRpcs);
        return new BindingRpcAdapterRegistration<>(implementation, domReg);
    }

    private Set<DOMRpcIdentifier> createDomRpcIdentifiers(final Class<? extends RpcService> type, final Set<YangInstanceIdentifier> paths) {
        final Set<SchemaPath> rpcs = getRpcSchemaPaths(type);

        final Set<DOMRpcIdentifier> ret = new HashSet<>();
        for(final YangInstanceIdentifier path : paths) {
            for(final SchemaPath rpc : rpcs) {
                ret.add(DOMRpcIdentifier.create(rpc, path));
            }
        }
        return ret;
    }

    private Set<YangInstanceIdentifier> toYangInstanceIdentifiers(final Set<InstanceIdentifier<?>> identifiers) {
        final Set<YangInstanceIdentifier> ret = new HashSet<>();
        for(final InstanceIdentifier<?> binding: identifiers) {
            ret.add(codec.toNormalized(binding));
        }
        return ret;
    }

    private Set<SchemaPath> getRpcSchemaPaths(final Class<? extends RpcService> type) {
        return codec.getRpcMethodToSchemaPath(type).values();
    }

}
