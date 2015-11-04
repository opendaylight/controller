/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.osgi;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.CheckedFuture;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.PackageTranslator;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
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
    private static final Logger LOG = LoggerFactory.getLogger(YangStoreSnapshot.class);


    private final Map<String /* Namespace from yang file */,
        Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> moduleMXBeanEntryMap;


    private final Map<QName, Map<String, ModuleMXBeanEntry>> qNamesToIdentitiesToModuleMXBeanEntries;

    private final SchemaContext schemaContext;
    private final BindingRuntimeContext bindingContextProvider;
    private final SchemaSourceProvider<YangTextSchemaSource> sourceProvider;

    public YangStoreSnapshot(final SchemaContext resolveSchemaContext,
        final BindingRuntimeContext bindingContextProvider,
        final SchemaSourceProvider<YangTextSchemaSource> sourceProvider) {
        this.bindingContextProvider = bindingContextProvider;
        this.sourceProvider = sourceProvider;
        LOG.trace("Resolved modules:{}", resolveSchemaContext.getModules());
        this.schemaContext = resolveSchemaContext;
        // JMX generator

        Map<String, String> namespaceToPackageMapping = Maps.newHashMap();
        PackageTranslator packageTranslator = new PackageTranslator(namespaceToPackageMapping);
        Map<QName, ServiceInterfaceEntry> qNamesToSIEs = new HashMap<>();
        Map<IdentitySchemaNode, ServiceInterfaceEntry> knownSEITracker = new HashMap<>();
        // create SIE structure qNamesToSIEs
        for (Module module : resolveSchemaContext.getModules()) {
            String packageName = packageTranslator.getPackageName(module);
            Map<QName, ServiceInterfaceEntry> namesToSIEntries = ServiceInterfaceEntry
                    .create(module, packageName, knownSEITracker);
            for (Entry<QName, ServiceInterfaceEntry> sieEntry : namesToSIEntries.entrySet()) {
                // merge value into qNamesToSIEs
                if (qNamesToSIEs.containsKey(sieEntry.getKey()) == false) {
                    qNamesToSIEs.put(sieEntry.getKey(), sieEntry.getValue());
                } else {
                    throw new IllegalStateException("Cannot add two SIE with same qname "
                            + sieEntry.getValue());
                }
            }
        }

        Map<String, Map<String, ModuleMXBeanEntry>> moduleMXBeanEntryMap = Maps.newHashMap();

        Map<QName, Map<String /* identity local name */, ModuleMXBeanEntry>> qNamesToIdentitiesToModuleMXBeanEntries = new HashMap<>();


        for (Module module : schemaContext.getModules()) {
            String packageName = packageTranslator.getPackageName(module);
            TypeProviderWrapper typeProviderWrapper = new TypeProviderWrapper(
                    new TypeProviderImpl(resolveSchemaContext));

            QName qName = QName.create(module.getNamespace(), module.getRevision(), module.getName());

            Map<String /* MB identity local name */, ModuleMXBeanEntry> namesToMBEs =
                    Collections.unmodifiableMap(ModuleMXBeanEntry.create(module, qNamesToSIEs, resolveSchemaContext,
                            typeProviderWrapper, packageName));
            moduleMXBeanEntryMap.put(module.getNamespace().toString(), namesToMBEs);

            qNamesToIdentitiesToModuleMXBeanEntries.put(qName, namesToMBEs);
        }
        this.moduleMXBeanEntryMap = Collections.unmodifiableMap(moduleMXBeanEntryMap);
        this.qNamesToIdentitiesToModuleMXBeanEntries = Collections.unmodifiableMap(qNamesToIdentitiesToModuleMXBeanEntries);

    }

    @Override
    public Map<String, Map<String, ModuleMXBeanEntry>> getModuleMXBeanEntryMap() {
        return moduleMXBeanEntryMap;
    }

    @Override
    public Map<QName, Map<String, ModuleMXBeanEntry>> getQNamesToIdentitiesToModuleMXBeanEntries() {
        return qNamesToIdentitiesToModuleMXBeanEntries;
    }

    @Override
    public Set<Module> getModules() {
        final Set<Module> modules = Sets.newHashSet(schemaContext.getModules());
        for (final Module module : schemaContext.getModules()) {
            modules.addAll(module.getSubmodules());
        }
        return modules;
    }

    @Override
    public String getModuleSource(final org.opendaylight.yangtools.yang.model.api.ModuleIdentifier moduleIdentifier) {
        final CheckedFuture<? extends YangTextSchemaSource, SchemaSourceException> source = sourceProvider.getSource(
            moduleIdentifier.getRevision() == null ?
                new SourceIdentifier(moduleIdentifier.getName()) :
                new SourceIdentifier(moduleIdentifier.getName(),
                    QName.formattedRevision(moduleIdentifier.getRevision())));

        try {
            final YangTextSchemaSource yangTextSchemaSource = source.checkedGet();
            return new String(ByteStreams.toByteArray(yangTextSchemaSource.openStream()), Charsets.UTF_8);
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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof YangStoreSnapshot)) {
            return false;
        }

        final YangStoreSnapshot that = (YangStoreSnapshot) o;
        if (schemaContext != null ? !schemaContext.equals(that.schemaContext) : that.schemaContext != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return schemaContext != null ? schemaContext.hashCode() : 0;
    }

    @Override
    public String fromYang(final String enumClass, final String enumYangValue) {
        Preconditions.checkState(bindingContextProvider != null, "Binding context provider was not set yet");
        final BiMap<String, String> enumMapping = bindingContextProvider.getEnumMapping(enumClass);
        final String javaName = enumMapping.get(enumYangValue);
        return Preconditions.checkNotNull(javaName, "Unable to resolve enum value %s for enum class %s with assumed enum mapping: %s", enumYangValue, enumClass, enumMapping);
    }

    @Override
    public String toYang(final String enumClass, final String enumJavaValue) {
        Preconditions.checkState(bindingContextProvider != null, "Binding context provider was not set yet");
        final BiMap<String, String> enumMapping = bindingContextProvider.getEnumMapping(enumClass);
        final String javaName = enumMapping.inverse().get(enumJavaValue);
        return Preconditions.checkNotNull(javaName,
            "Unable to map enum value %s for enum class %s with assumed enum mapping: %s", enumJavaValue, enumClass,
            enumMapping.inverse());
    }
}
