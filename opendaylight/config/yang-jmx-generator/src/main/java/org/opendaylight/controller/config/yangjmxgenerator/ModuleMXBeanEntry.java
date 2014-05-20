/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.FullyQualifiedNameHelper;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Represents part of yang model that describes a module.
 *
 * Example:
 * <p>
 * <blockquote>
 *
 * <pre>
 *  identity threadpool-dynamic {
 *      base config:module-type;
 *      description "threadpool-dynamic description";
 *      config:provided-service "th2:threadpool";
 *      config:provided-service "th2:scheduled-threadpool";
 *      config:java-name-prefix DynamicThreadPool
 *  }
 *  augment "/config:modules/config:module/config:module-type" {
 *     case threadpool-dynamic {
 *         when "/config:modules/config:module/config:module-type = 'threadpool-dynamic'";
 *
 *         container "configuration" {
 *             // regular java attribute
 *             leaf core-size {
 *                 type uint32;
 *          }
 *
 *             ...
 *          // dependency
 *             container threadfactory {
 *                 uses config:service-ref {
 *                     refine type {
 *                         config:required-identity th:threadfactory;
 *                  }
 *              }
 *          }
 *      }
 * }
 * </pre>
 *
 * </blockquote>
 * </p>
 */
public class ModuleMXBeanEntry extends AbstractEntry {

    private static final String MODULE_SUFFIX = "Module";
    private static final String FACTORY_SUFFIX = MODULE_SUFFIX + "Factory";
    private static final String CLASS_NAME_SUFFIX = MODULE_SUFFIX + "MXBean";
    private static final String ABSTRACT_PREFIX = "Abstract";

    private final ModuleMXBeanEntryInitial initial;

    private Map<String, AttributeIfc> yangToAttributes;

    private final Map<String, QName> providedServices;

    private Collection<RuntimeBeanEntry> runtimeBeans;
    private String nullableDummyContainerName;

    ModuleMXBeanEntry(ModuleMXBeanEntryInitial initials, Map<String, AttributeIfc> yangToAttributes,
            Map<String, QName> providedServices2, Collection<RuntimeBeanEntry> runtimeBeans) {
        this.yangToAttributes = yangToAttributes;
        this.providedServices = Collections.unmodifiableMap(providedServices2);
        this.runtimeBeans = runtimeBeans;
        this.initial = initials;
    }

    public String getMXBeanInterfaceName() {
        return initial.javaNamePrefix + CLASS_NAME_SUFFIX;
    }

    public String getStubFactoryName() {
        return initial.javaNamePrefix + FACTORY_SUFFIX;
    }

    public String getAbstractFactoryName() {
        return ABSTRACT_PREFIX + getStubFactoryName();
    }

    public String getStubModuleName() {
        return initial.javaNamePrefix + MODULE_SUFFIX;
    }

    public String getAbstractModuleName() {
        return ABSTRACT_PREFIX + getStubModuleName();
    }

    public String getFullyQualifiedName(String typeName) {
        return FullyQualifiedNameHelper.getFullyQualifiedName(initial.packageName,
                typeName);
    }

    public String getGloballyUniqueName() {
        return initial.localName;
    }

    public String getPackageName() {
        return initial.packageName;
    }

    /**
     * @return services implemented by this module. Keys are fully qualified
     *         java names of generated ServiceInterface classes, values are
     *         identity local names.
     */
    public Map<String, QName> getProvidedServices() {
        return providedServices;
    }

    public void setRuntimeBeans(Collection<RuntimeBeanEntry> newRuntimeBeans) {
        runtimeBeans = newRuntimeBeans;
    }

    public Collection<RuntimeBeanEntry> getRuntimeBeans() {
        return runtimeBeans;
    }

    public String getJavaNamePrefix() {
        return initial.javaNamePrefix;
    }

    public String getNamespace() {
        return initial.namespace;
    }

    /**
     * Transform module to zero or more ModuleMXBeanEntry instances. Each
     * instance must have a globally unique local name.
     *
     * @return Map of identity local names as keys, and ModuleMXBeanEntry
     *         instances as values
     */
    public static Map<String/* identity local name */, ModuleMXBeanEntry> create(
            Module currentModule,
            Map<QName, ServiceInterfaceEntry> qNamesToSIEs,
            SchemaContext schemaContext,
            TypeProviderWrapper typeProviderWrapper, String packageName) {

        ModuleMXBeanEntryBuilder builder = new ModuleMXBeanEntryBuilder().setModule(currentModule).setqNamesToSIEs(qNamesToSIEs)
                .setSchemaContext(schemaContext).setTypeProviderWrapper(typeProviderWrapper)
                .setPackageName(packageName);

        return builder.build();
    }

    public Map<String, AttributeIfc> getAttributes() {
        return yangToAttributes;
    }

    void setYangToAttributes(Map<String, AttributeIfc> newAttributes) {
        this.yangToAttributes = newAttributes;
    }

    public String getNullableDescription() {
        return initial.description;
    }

    public QName getYangModuleQName() {
        return initial.qName;
    }

    @Override
    public String toString() {
        return "ModuleMXBeanEntry{" + "globallyUniqueName='"
                + initial.localName + '\'' + ", packageName='" + initial.packageName
                + '\'' + '}';
    }

    public String getNullableDummyContainerName() {
        return nullableDummyContainerName;
    }

    public void setNullableDummyContainerName(String nullableDummyContainerName) {
        this.nullableDummyContainerName = nullableDummyContainerName;
    }


    static final class ModuleMXBeanEntryInitial {

        private String localName;
        private String description;
        private String packageName;
        private String javaNamePrefix;
        private String namespace;
        private QName qName;

        ModuleMXBeanEntryInitial(String localName, String description, String packageName, String javaNamePrefix, String namespace, QName qName) {
            this.localName = localName;
            this.description = description;
            this.packageName = packageName;
            this.javaNamePrefix = javaNamePrefix;
            this.namespace = namespace;
            this.qName = qName;
        }
    }

    static final class ModuleMXBeanEntryInitialBuilder {
        private String localName;
        private String description;
        private String packageName;
        private String javaNamePrefix;
        private String namespace;
        private QName qName;

        public ModuleMXBeanEntryInitialBuilder setPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public ModuleMXBeanEntryInitialBuilder setJavaNamePrefix(String javaNamePrefix) {
            this.javaNamePrefix = javaNamePrefix;
            return this;
        }

        public ModuleMXBeanEntryInitialBuilder setNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public ModuleMXBeanEntryInitialBuilder setqName(QName qName) {
            this.qName = qName;
            return this;
        }

        public ModuleMXBeanEntry.ModuleMXBeanEntryInitial build() {
            return new ModuleMXBeanEntry.ModuleMXBeanEntryInitial(localName, description, packageName, javaNamePrefix, namespace, qName);
        }

        public ModuleMXBeanEntryInitialBuilder setIdSchemaNode(IdentitySchemaNode idSchemaNode) {
            this.localName = idSchemaNode.getQName().getLocalName();
            this.description = idSchemaNode.getDescription();
            return this;
        }

    }
}
