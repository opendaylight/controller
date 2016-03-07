/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.AbstractSchemaAwareTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeLeafOnlyAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.rpcservice.rev140701.OpendaylightTestRpcServiceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.util.AbstractSchemaContext;
import javassist.ClassPool;

public class BindingNormalizedCodecTest extends AbstractSchemaAwareTest {

    private static final TopLevelListKey TOP_FOO_KEY = new TopLevelListKey("foo");
    private static final InstanceIdentifier<TopLevelList> BA_TOP_LEVEL_LIST = InstanceIdentifier
            .builder(Top.class).child(TopLevelList.class, TOP_FOO_KEY).build();
    private static final InstanceIdentifier<TreeLeafOnlyAugment> BA_TREE_LEAF_ONLY = BA_TOP_LEVEL_LIST.augmentation(TreeLeafOnlyAugment.class);
    private static final InstanceIdentifier<TreeComplexUsesAugment> BA_TREE_COMPLEX_USES = BA_TOP_LEVEL_LIST.augmentation(TreeComplexUsesAugment.class);
    private static final QName SIMPLE_VALUE_QNAME = QName.create(TreeComplexUsesAugment.QNAME, "simple-value");
    private static final QName NAME_QNAME = QName.create(Top.QNAME, "name");
    private static final YangInstanceIdentifier BI_TOP_LEVEL_LIST = YangInstanceIdentifier.builder().
            node(Top.QNAME).node(TopLevelList.QNAME).nodeWithKey(
                    TopLevelList.QNAME, NAME_QNAME, TOP_FOO_KEY.getName()).build();


    private BindingToNormalizedNodeCodec codec;
    private SchemaContext context;

    @Override
    protected void setupWithSchema(final SchemaContext context) {
        this.context = context;
        final DataObjectSerializerGenerator streamWriter = StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()));
        final BindingNormalizedNodeCodecRegistry registry = new BindingNormalizedNodeCodecRegistry(streamWriter);
        codec = new BindingToNormalizedNodeCodec(GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy(), registry, true);
    };

    @Test
    public void testComplexAugmentationSerialization() {
        codec.onGlobalContextUpdated(context);
        final PathArgument lastArg = codec.toYangInstanceIdentifier(BA_TREE_COMPLEX_USES).getLastPathArgument();
        assertTrue(lastArg instanceof AugmentationIdentifier);
    }


    @Test
    public void testLeafOnlyAugmentationSerialization() {
        codec.onGlobalContextUpdated(context);
        final PathArgument leafOnlyLastArg = codec.toYangInstanceIdentifier(BA_TREE_LEAF_ONLY).getLastPathArgument();
        assertTrue(leafOnlyLastArg instanceof AugmentationIdentifier);
        assertTrue(((AugmentationIdentifier) leafOnlyLastArg).getPossibleChildNames().contains(SIMPLE_VALUE_QNAME));
    }

    @Test
    public void testToYangInstanceIdentifierBlocking() {
        codec.onGlobalContextUpdated(new EmptySchemaContext());

        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<YangInstanceIdentifier> yangId = new AtomicReference<>();
        final AtomicReference<RuntimeException> error = new AtomicReference<>();
        new Thread() {
            @Override
            public void run() {
                try {
                    yangId.set(codec.toYangInstanceIdentifierBlocking(BA_TOP_LEVEL_LIST));
                } catch(RuntimeException e) {
                    error.set(e);
                } finally {
                    done.countDown();
                }
            }
        }.start();

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        codec.onGlobalContextUpdated(context);

        assertEquals("toYangInstanceIdentifierBlocking completed", true,
                Uninterruptibles.awaitUninterruptibly(done, 3, TimeUnit.SECONDS));
        if(error.get() != null) {
            throw error.get();
        }

        assertEquals("toYangInstanceIdentifierBlocking", BI_TOP_LEVEL_LIST, yangId.get());
    }

    @Test
    public void testGetRpcMethodToSchemaPathWithNoInitialSchemaContext() {
        testGetRpcMethodToSchemaPath();
    }

    @Test
    public void testGetRpcMethodToSchemaPathBlocking() {
        codec.onGlobalContextUpdated(new EmptySchemaContext());
        testGetRpcMethodToSchemaPath();
    }

    private void testGetRpcMethodToSchemaPath() {
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<ImmutableBiMap<Method, SchemaPath>> retMap = new AtomicReference<>();
        final AtomicReference<RuntimeException> error = new AtomicReference<>();
        new Thread() {
            @Override
            public void run() {
                try {
                    retMap.set(codec.getRpcMethodToSchemaPath(OpendaylightTestRpcServiceService.class));
                } catch(RuntimeException e) {
                    error.set(e);
                } finally {
                    done.countDown();
                }
            }
        }.start();

        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        codec.onGlobalContextUpdated(context);

        assertEquals("getRpcMethodToSchemaPath completed", true,
                Uninterruptibles.awaitUninterruptibly(done, 3, TimeUnit.SECONDS));
        if(error.get() != null) {
            throw error.get();
        }

        for(Method method: retMap.get().keySet()) {
            if(method.getName().equals("rockTheHouse")) {
                return;
            }
        }

        fail("rockTheHouse RPC method not found");
    }

    static class EmptySchemaContext extends AbstractSchemaContext {
        @Override
        public Set<Module> getModules() {
            return Collections.emptySet();
        }

        @Override
        protected Map<ModuleIdentifier, String> getIdentifiersToSources() {
            return Collections.emptyMap();
        }

        @Override
        protected SetMultimap<URI, Module> getNamespaceToModules() {
            return Multimaps.forMap(Collections.emptyMap());
        }

        @Override
        protected SetMultimap<String, Module> getNameToModules() {
            return Multimaps.forMap(Collections.emptyMap());
        }
    }
}
