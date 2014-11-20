/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.opendaylight.controller.config.yangjmxgenerator.ConfigConstants.SERVICE_TYPE_Q_NAME;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents identity derived from {@link ConfigConstants#SERVICE_TYPE_Q_NAME}.
 * Example:
 * <p>
 * <blockquote>
 *
 * <pre>
 *  identity eventbus {
 *  description
 *  "Service representing an event bus. The service acts as message
 *  router between event producers and event consumers";
 *
 *  base "config:service-type";
 *  config:java-class "com.google.common.eventbus.EventBus";
 *  }
 * </pre>
 *
 * </blockquote>
 * </p>
 */
public class ServiceInterfaceEntry extends AbstractEntry {
    private static final Logger LOG = LoggerFactory
            .getLogger(ServiceInterfaceEntry.class);

    private static final String CLASS_NAME_SUFFIX = "ServiceInterface";
    private final Optional<ServiceInterfaceEntry> maybeBaseCache;
    private final String exportedOsgiClassName;
    private final QName qName;
    private final String nullableDescription, packageName, typeName;
    private final QName yangModuleQName;

    private ServiceInterfaceEntry(IdentitySchemaNode id, String packageName, QName yangModuleQName) {
        this(Optional.<ServiceInterfaceEntry> absent(), id, packageName, yangModuleQName);
    }

    private ServiceInterfaceEntry(Optional<ServiceInterfaceEntry> base,
            IdentitySchemaNode id, String packageName, QName yangModuleQName) {
        checkNotNull(base);
        this.maybeBaseCache = base;
        List<UnknownSchemaNode> unknownSchemaNodes = id.getUnknownSchemaNodes();
        List<String> exportedOsgiClassNames = new ArrayList<>(
                unknownSchemaNodes.size());
        for (UnknownSchemaNode usn : unknownSchemaNodes) {
            if (ConfigConstants.JAVA_CLASS_EXTENSION_QNAME.equals(usn
                    .getNodeType())) {
                String localName = usn.getNodeParameter();
                exportedOsgiClassNames.add(localName);
            } else {
                throw new IllegalStateException(format(
                        "Unexpected unknown schema node. Expected %s, got %s",
                        ConfigConstants.JAVA_CLASS_EXTENSION_QNAME,
                        usn.getNodeType()));
            }
        }
        if (exportedOsgiClassNames.size() != 1) {
            throw new IllegalArgumentException(
                    format("Cannot find one to one mapping from %s to "
                            + "java class defined by %s language extension in %s",
                            getClass(),
                            ConfigConstants.JAVA_CLASS_EXTENSION_QNAME, id));
        }
        this.exportedOsgiClassName = exportedOsgiClassNames.get(0);
        qName = id.getQName();
        nullableDescription = id.getDescription();
        typeName = getSimpleName(exportedOsgiClassName) + CLASS_NAME_SUFFIX;
        this.packageName = packageName;
        this.yangModuleQName = yangModuleQName;
    }

    private static final String getSimpleName(String fullyQualifiedName) {
        int lastDotPosition = fullyQualifiedName.lastIndexOf(".");
        return fullyQualifiedName.substring(lastDotPosition + 1);
    }

    public String getNullableDescription() {
        return nullableDescription;
    }

    public Optional<ServiceInterfaceEntry> getBase() {
        return maybeBaseCache;
    }

    public String getExportedOsgiClassName() {
        return exportedOsgiClassName;
    }

    public QName getQName() {
        return qName;
    }

    /**
     * @return Map of QNames as keys and ServiceInterfaceEntry instances as
     *         values
     */
    public static Map<QName, ServiceInterfaceEntry> create(Module currentModule,
            String packageName,Map<IdentitySchemaNode, ServiceInterfaceEntry> definedSEItracker) {
        LOG.debug("Generating ServiceInterfaces from {} to package {}",
                currentModule.getNamespace(), packageName);

        Map<IdentitySchemaNode, ServiceInterfaceEntry> identitiesToSIs = new HashMap<>();
        Set<IdentitySchemaNode> notVisited = new HashSet<>(
                currentModule.getIdentities());
        int lastSize = notVisited.size() + 1;
        while (!notVisited.isEmpty()) {
            if (notVisited.size() == lastSize) {
                LOG.debug(
                        "Following identities will be ignored while generating ServiceInterfaces, as they are not derived from {} : {}",
                        SERVICE_TYPE_Q_NAME, notVisited);
                break;
            }
            lastSize = notVisited.size();
            for (Iterator<IdentitySchemaNode> iterator = notVisited.iterator(); iterator
                    .hasNext();) {
                IdentitySchemaNode identity = iterator.next();
                ServiceInterfaceEntry created = null;
                if (identity.getBaseIdentity() == null) {
                    // this can happen while loading config module, just skip
                    // the identity
                    continue;
                } else if (identity.getBaseIdentity().getQName()
                        .equals(SERVICE_TYPE_Q_NAME)) {
                    // this is a base type
                    created = new ServiceInterfaceEntry(identity, packageName, ModuleUtil.getQName(currentModule));
                } else {
                    ServiceInterfaceEntry foundBase = definedSEItracker
                            .get(identity.getBaseIdentity());
                    // derived type, did we convert the parent?
                    if (foundBase != null) {
                        created = new ServiceInterfaceEntry(
                                Optional.of(foundBase), identity, packageName, ModuleUtil.getQName(currentModule));
                    }
                }


                if (created != null) {
                    created.setYangModuleName(currentModule.getName());
                    // TODO how to get local name
                    created.setYangModuleLocalname(identity.getQName()
                            .getLocalName());
                    identitiesToSIs.put(identity, created);
                    definedSEItracker.put(identity, created);
                    iterator.remove();
                }
            }
        }
        // create result map
        Map<QName, ServiceInterfaceEntry> resultMap = new HashMap<>();
        for (ServiceInterfaceEntry sie : identitiesToSIs.values()) {
            resultMap.put(sie.getQName(), sie);
        }
        LOG.debug("Number of ServiceInterfaces to be generated: {}",
                resultMap.size());
        return resultMap;
    }

    public String getFullyQualifiedName() {
        return packageName + "." + typeName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getTypeName() {
        return typeName;
    }

    public QName getYangModuleQName() {
        return yangModuleQName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServiceInterfaceEntry that = (ServiceInterfaceEntry) o;

        if (!maybeBaseCache.equals(that.maybeBaseCache)) {
            return false;
        }
        if (!nullableDescription.equals(that.nullableDescription)) {
            return false;
        }
        if (!exportedOsgiClassName.equals(that.exportedOsgiClassName)) {
            return false;
        }
        if (!qName.equals(that.qName)) {
            return false;
        }
        if (!packageName.equals(that.packageName)) {
            return false;
        }
        if (!typeName.equals(that.typeName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = maybeBaseCache.hashCode();
        result = 31 * result + exportedOsgiClassName.hashCode();
        result = 31 * result + nullableDescription.hashCode();
        result = 31 * result + typeName.hashCode();
        result = 31 * result + packageName.hashCode();
        result = 31 * result + qName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ServiceInterfaceEntry{" + "maybeBaseCache=" + maybeBaseCache
                + ", qName='" + qName + '\'' + ", fullyQualifiedName='"
                + getFullyQualifiedName() + '\'' + ", exportedOsgiClassName="
                + exportedOsgiClassName + ", nullableDescription='"
                + nullableDescription + '\'' + '}';
    }
}
