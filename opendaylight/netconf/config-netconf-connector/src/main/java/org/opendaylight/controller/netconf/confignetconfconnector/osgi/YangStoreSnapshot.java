/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.PackageTranslator;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChangeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.changed.by.parms.ChangedByBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.changed.by.parms.changed.by.server.or.user.ServerBuilder;
import org.opendaylight.yangtools.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class YangStoreSnapshot implements YangStoreContext {
    private static final Logger LOG = LoggerFactory.getLogger(YangStoreSnapshot.class);


    private final Map<String /* Namespace from yang file */,
    Map<String /* Name of module entry from yang file */, ModuleMXBeanEntry>> moduleMXBeanEntryMap;


    private final Map<QName, Map<String, ModuleMXBeanEntry>> qNamesToIdentitiesToModuleMXBeanEntries;

    private final SchemaContext schemaContext;
    private static final Function<Module, Uri> MODULE_TO_URI = new Function<Module, Uri>() {
        @Override
        public Uri apply(final Module input) {
            final QName qName = QName.cachedReference(QName.create(input.getQNameModule(), input.getName()));
            return new Uri(qName.toString());
        }
    };


    public YangStoreSnapshot(final SchemaContext resolveSchemaContext) {
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
        return schemaContext.getModules();
    }

    @Override
    public String getModuleSource(final org.opendaylight.yangtools.yang.model.api.ModuleIdentifier moduleIdentifier) {
        return schemaContext.getModuleSource(moduleIdentifier).get();
    }

    NetconfCapabilityChange computeDiff(final YangStoreSnapshot previous) {
        final Sets.SetView<Module> removed = Sets.difference(previous.getModules(), getModules());
        final Sets.SetView<Module> added = Sets.difference(getModules(), previous.getModules());

        final NetconfCapabilityChangeBuilder netconfCapabilityChangeBuilder = new NetconfCapabilityChangeBuilder();
        netconfCapabilityChangeBuilder.setChangedBy(new ChangedByBuilder().setServerOrUser(new ServerBuilder().setServer(true).build()).build());
        netconfCapabilityChangeBuilder.setDeletedCapability(Lists.newArrayList(Collections2.transform(removed, MODULE_TO_URI)));
        netconfCapabilityChangeBuilder.setAddedCapability(Lists.newArrayList(Collections2.transform(added, MODULE_TO_URI)));
        // TODO modified should be computed ... but why ?
        netconfCapabilityChangeBuilder.setModifiedCapability(Collections.<Uri>emptyList());
        return netconfCapabilityChangeBuilder.build();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final YangStoreSnapshot that = (YangStoreSnapshot) o;

        if (schemaContext != null ? !schemaContext.equals(that.schemaContext) : that.schemaContext != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return schemaContext != null ? schemaContext.hashCode() : 0;
    }
}
