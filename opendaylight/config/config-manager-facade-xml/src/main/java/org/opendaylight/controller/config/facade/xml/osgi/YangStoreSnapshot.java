/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.osgi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.CheckedFuture;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.PackageTranslator;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class YangStoreSnapshot implements YangStoreContext, EnumResolver {
    private static final class MXBeans {
        private final Map<String /* Namespace from yang file */,
                Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> moduleMXBeanEntryMap;
        private final Map<QName, Map<String, ModuleMXBeanEntry>> qNamesToIdentitiesToModuleMXBeanEntries;

        MXBeans(final SchemaContext schemaContext) {
            LOG.trace("Resolved modules:{}", schemaContext.getModules());

            // JMX generator
            final Map<String, String> namespaceToPackageMapping = Maps.newHashMap();
            final PackageTranslator packageTranslator = new PackageTranslator(namespaceToPackageMapping);
            final Map<QName, ServiceInterfaceEntry> qNamesToSIEs = new HashMap<>();
            final Map<IdentitySchemaNode, ServiceInterfaceEntry> knownSEITracker = new HashMap<>();
            // create SIE structure qNamesToSIEs
            for (final Module module : schemaContext.getModules()) {
                final String packageName = packageTranslator.getPackageName(module);
                final Map<QName, ServiceInterfaceEntry> namesToSIEntries = ServiceInterfaceEntry
                        .create(module, packageName, knownSEITracker);
                for (final Entry<QName, ServiceInterfaceEntry> sieEntry : namesToSIEntries.entrySet()) {
                    // merge value into qNamesToSIEs
                    if (qNamesToSIEs.containsKey(sieEntry.getKey()) == false) {
                        qNamesToSIEs.put(sieEntry.getKey(), sieEntry.getValue());
                    } else {
                        throw new IllegalStateException("Cannot add two SIE with same qname "
                                + sieEntry.getValue());
                    }
                }
            }

            final Map<String, Map<String, ModuleMXBeanEntry>> moduleMXBeanEntryMap = Maps.newHashMap();

            final Map<QName, Map<String /* identity local name */, ModuleMXBeanEntry>> qNamesToIdentitiesToModuleMXBeanEntries = new HashMap<>();


            for (final Module module : schemaContext.getModules()) {
                final String packageName = packageTranslator.getPackageName(module);
                final TypeProviderWrapper typeProviderWrapper = new TypeProviderWrapper(
                        new TypeProviderImpl(schemaContext));

                final QName qName = QName.create(module.getNamespace(), module.getRevision(), module.getName());

                final Map<String /* MB identity local name */, ModuleMXBeanEntry> namesToMBEs =
                        Collections.unmodifiableMap(ModuleMXBeanEntry.create(module, qNamesToSIEs, schemaContext,
                                typeProviderWrapper, packageName));
                moduleMXBeanEntryMap.put(module.getNamespace().toString(), namesToMBEs);

                qNamesToIdentitiesToModuleMXBeanEntries.put(qName, namesToMBEs);
            }
            this.moduleMXBeanEntryMap = Collections.unmodifiableMap(moduleMXBeanEntryMap);
            this.qNamesToIdentitiesToModuleMXBeanEntries = Collections.unmodifiableMap(qNamesToIdentitiesToModuleMXBeanEntries);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(YangStoreSnapshot.class);
    private final SchemaSourceProvider<YangTextSchemaSource> sourceProvider;
    private final BindingRuntimeContext bindingContextProvider;

    /**
     * We want to lazily compute the context of the MXBean class and have it only softly-attached to this instance,
     * so it can be garbage collected when the memory gets tight. If the schema context changes as we are computing
     * things, YangStoreService will detect that and retry, so we do not have to worry about that.
     */
    private volatile SoftReference<MXBeans> ref = new SoftReference<>(null);

    public YangStoreSnapshot(final BindingRuntimeContext bindingContextProvider,
        final SchemaSourceProvider<YangTextSchemaSource> sourceProvider) {
        this.bindingContextProvider = Preconditions.checkNotNull(bindingContextProvider);
        this.sourceProvider = Preconditions.checkNotNull(sourceProvider);
    }

    private MXBeans getMXBeans() {
        MXBeans mxBean = this.ref.get();

        if (mxBean == null) {
            synchronized (this) {
                mxBean = this.ref.get();
                if (mxBean == null) {
                    mxBean = new MXBeans(this.bindingContextProvider.getSchemaContext());
                    this.ref = new SoftReference<>(mxBean);
                }
            }
        }

        return mxBean;
    }

    @Override
    public Map<String, Map<String, ModuleMXBeanEntry>> getModuleMXBeanEntryMap() {
        return getMXBeans().moduleMXBeanEntryMap;
    }

    @Override
    public Map<QName, Map<String, ModuleMXBeanEntry>> getQNamesToIdentitiesToModuleMXBeanEntries() {
        return getMXBeans().qNamesToIdentitiesToModuleMXBeanEntries;
    }

    @Override
    public Set<Module> getModules() {
        final Set<Module> modules = Sets.newHashSet(this.bindingContextProvider.getSchemaContext().getModules());
        for (final Module module : this.bindingContextProvider.getSchemaContext().getModules()) {
            modules.addAll(module.getSubmodules());
        }
        return modules;
    }

    @Override
    public String getModuleSource(final org.opendaylight.yangtools.yang.model.api.ModuleIdentifier moduleIdentifier) {
        final CheckedFuture<? extends YangTextSchemaSource, SchemaSourceException> source = this.sourceProvider.getSource(
            SourceIdentifier.create(moduleIdentifier.getName(), Optional.fromNullable(
                QName.formattedRevision(moduleIdentifier.getRevision()))));

        try {
            final YangTextSchemaSource yangTextSchemaSource = source.checkedGet();
            try(InputStream inStream = yangTextSchemaSource.openStream()) {
                return new String(ByteStreams.toByteArray(inStream), StandardCharsets.UTF_8);
            }
        } catch (SchemaSourceException | IOException e) {
            LOG.warn("Unable to provide source for {}", moduleIdentifier, e);
            throw new IllegalArgumentException("Unable to provide source for " + moduleIdentifier, e);
        }
    }

    @Override
    public EnumResolver getEnumResolver() {
        return this;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof YangStoreSnapshot)) {
            return false;
        }

        final YangStoreSnapshot other = (YangStoreSnapshot) obj;
        return Objects.equals(this.bindingContextProvider, other.bindingContextProvider);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.bindingContextProvider);
    }

    @Override
    public String fromYang(final String enumClass, final String enumYangValue) {
        Preconditions.checkState(this.bindingContextProvider != null, "Binding context provider was not set yet");
        final BiMap<String, String> enumMapping = this.bindingContextProvider.getEnumMapping(enumClass);
        final String javaName = enumMapping.get(enumYangValue);
        return Preconditions.checkNotNull(javaName, "Unable to resolve enum value %s for enum class %s with assumed enum mapping: %s", enumYangValue, enumClass, enumMapping);
    }

    @Override
    public String toYang(final String enumClass, final String enumJavaValue) {
        Preconditions.checkState(this.bindingContextProvider != null, "Binding context provider was not set yet");
        final BiMap<String, String> enumMapping = this.bindingContextProvider.getEnumMapping(enumClass);
        final String javaName = enumMapping.inverse().get(enumJavaValue);
        return Preconditions.checkNotNull(javaName,
            "Unable to map enum value %s for enum class %s with assumed enum mapping: %s", enumJavaValue, enumClass,
            enumMapping.inverse());
    }
}
