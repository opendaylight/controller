/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.FullyQualifiedNameHelper;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.NameConflictException;
import org.opendaylight.yangtools.binding.generator.util.BindingGeneratorUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleImport;
import org.opendaylight.yangtools.yang.model.api.RevisionAwareXPath;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.opendaylight.controller.config.yangjmxgenerator.ConfigConstants.createConfigQName;

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
    private static final Logger logger = LoggerFactory
            .getLogger(ModuleMXBeanEntry.class);

    // TODO: the XPath should be parsed by code generator IMO
    private static final String MAGIC_STRING = "MAGIC_STRING";
    private static final String MODULE_CONDITION_XPATH_TEMPLATE = "^/MAGIC_STRING:modules/MAGIC_STRING:module/MAGIC_STRING:type\\s*=\\s*['\"](.+)['\"]$";
    private static final SchemaPath expectedConfigurationAugmentationSchemaPath = new SchemaPath(
            Arrays.asList(createConfigQName("modules"),
                    createConfigQName("module"),
                    createConfigQName("configuration")), true);
    private static final SchemaPath expectedStateAugmentationSchemaPath = new SchemaPath(
            Arrays.asList(createConfigQName("modules"),
                    createConfigQName("module"), createConfigQName("state")),
            true);

    private static final Pattern PREFIX_COLON_LOCAL_NAME = Pattern
            .compile("^(.+):(.+)$");

    private static final String MODULE_SUFFIX = "Module";
    private static final String FACTORY_SUFFIX = MODULE_SUFFIX + "Factory";
    private static final String CLASS_NAME_SUFFIX = MODULE_SUFFIX + "MXBean";
    private static final String ABSTRACT_PREFIX = "Abstract";

    /*
     * threadpool-dynamic from the example above, taken from when condition, not
     * the case name
     */
    private final String globallyUniqueName;

    private Map<String, AttributeIfc> yangToAttributes;

    private final String nullableDescription, packageName, javaNamePrefix,
            namespace;

    private final Map<String, QName> providedServices;

    private Collection<RuntimeBeanEntry> runtimeBeans;

    public ModuleMXBeanEntry(IdentitySchemaNode id,
            Map<String, AttributeIfc> yangToAttributes, String packageName,
            Map<String, QName> providedServices2, String javaNamePrefix,
            String namespace, Collection<RuntimeBeanEntry> runtimeBeans) {
        this.globallyUniqueName = id.getQName().getLocalName();
        this.yangToAttributes = yangToAttributes;
        this.nullableDescription = id.getDescription();
        this.packageName = packageName;
        this.javaNamePrefix = checkNotNull(javaNamePrefix);
        this.namespace = checkNotNull(namespace);
        this.providedServices = Collections.unmodifiableMap(providedServices2);
        this.runtimeBeans = runtimeBeans;
    }

    public String getMXBeanInterfaceName() {
        return javaNamePrefix + CLASS_NAME_SUFFIX;
    }

    public String getStubFactoryName() {
        return javaNamePrefix + FACTORY_SUFFIX;
    }

    public String getAbstractFactoryName() {
        return ABSTRACT_PREFIX + getStubFactoryName();
    }

    public String getStubModuleName() {
        return javaNamePrefix + MODULE_SUFFIX;
    }

    public String getAbstractModuleName() {
        return ABSTRACT_PREFIX + getStubModuleName();
    }

    public String getFullyQualifiedName(String typeName) {
        return FullyQualifiedNameHelper.getFullyQualifiedName(packageName,
                typeName);
    }

    public String getGloballyUniqueName() {
        return globallyUniqueName;
    }

    public String getPackageName() {
        return packageName;
    }

    /**
     * @return services implemented by this module. Keys are fully qualified java names of generated
     * ServiceInterface classes, values are identity local names.
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
        return javaNamePrefix;
    }

    public String getNamespace() {
        return namespace;
    }

    @VisibleForTesting
    static Matcher getWhenConditionMatcher(String prefix,
            RevisionAwareXPath whenConstraint) {
        String xpathRegex = MODULE_CONDITION_XPATH_TEMPLATE.replace(
                MAGIC_STRING, prefix);
        Pattern pattern = Pattern.compile(xpathRegex);
        return pattern.matcher(whenConstraint.toString());
    }

    static String getConfigModulePrefixFromImport(Module currentModule) {
        for (ModuleImport currentImport : currentModule.getImports()) {
            if (currentImport.getModuleName().equals(
                    ConfigConstants.CONFIG_MODULE)) {
                return currentImport.getPrefix();
            }
        }
        throw new IllegalArgumentException("Cannot find import "
                + ConfigConstants.CONFIG_MODULE + " in " + currentModule);
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
        Map<String, QName> uniqueGeneratedClassesNames = new HashMap<>();
        logger.debug("Generating ModuleMXBeans of {} to package {}",
                currentModule.getNamespace(), packageName);
        String configModulePrefix;
        try {
            configModulePrefix = getConfigModulePrefixFromImport(currentModule);
        } catch (IllegalArgumentException e) {
            // this module does not import config module
            return Collections.emptyMap();
        }

        // get identities of base config:module-type
        Map<String, IdentitySchemaNode> moduleIdentities = new HashMap<>();

        for (IdentitySchemaNode id : currentModule.getIdentities()) {
            if (id.getBaseIdentity() != null
                    && ConfigConstants.MODULE_TYPE_Q_NAME.equals(id
                            .getBaseIdentity().getQName())) {
                String identityLocalName = id.getQName().getLocalName();
                if (moduleIdentities.containsKey(identityLocalName)) {
                    throw new IllegalStateException(
                            "Module name already defined in this module: "
                                    + identityLocalName);
                } else {
                    moduleIdentities.put(identityLocalName, id);
                    logger.debug("Found identity {}", identityLocalName);
                }
                // validation check on unknown schema nodes
                boolean providedServiceWasSet = false;
                for (UnknownSchemaNode unknownNode : id.getUnknownSchemaNodes()) {
                    // TODO: test this
                    if (ConfigConstants.PROVIDED_SERVICE_EXTENSION_QNAME
                            .equals(unknownNode.getNodeType())) {
                        // no op: 0 or more provided identities are allowed
                    } else if (ConfigConstants.JAVA_NAME_PREFIX_EXTENSION_QNAME
                            .equals(unknownNode.getNodeType())) {
                        // 0..1 allowed
                        checkState(
                                providedServiceWasSet == false,
                                format("More than one language extension %s is not allowed here: %s",
                                        ConfigConstants.JAVA_NAME_PREFIX_EXTENSION_QNAME,
                                        id));
                        providedServiceWasSet = true;
                    } else {
                        throw new IllegalStateException(
                                "Unexpected language extension "
                                        + unknownNode.getNodeType());
                    }
                }
            }
        }
        Map<String, ModuleMXBeanEntry> result = new HashMap<>();
        // each module name should have an augmentation defined
        Map<String, IdentitySchemaNode> unaugmentedModuleIdentities = new HashMap<>(
                moduleIdentities);
        for (AugmentationSchema augmentation : currentModule.getAugmentations()) {
            Set<DataSchemaNode> childNodes = augmentation.getChildNodes();
            if (childNodes.size() == 1) {
                DataSchemaNode when = childNodes.iterator().next();
                if (when instanceof ChoiceCaseNode) {
                    ChoiceCaseNode choiceCaseNode = (ChoiceCaseNode) when;
                    if (choiceCaseNode.getConstraints() == null
                            || choiceCaseNode.getConstraints()
                                    .getWhenCondition() == null) {
                        continue;
                    }
                    RevisionAwareXPath xPath = choiceCaseNode.getConstraints()
                            .getWhenCondition();
                    Matcher matcher = getWhenConditionMatcher(
                            configModulePrefix, xPath);
                    if (matcher.matches() == false) {
                        continue;
                    }
                    String moduleLocalNameFromXPath = matcher.group(1);
                    IdentitySchemaNode moduleIdentity = moduleIdentities
                            .get(moduleLocalNameFromXPath);
                    unaugmentedModuleIdentities
                            .remove(moduleLocalNameFromXPath);
                    checkState(moduleIdentity != null, "Cannot find identity "
                            + moduleLocalNameFromXPath
                            + " matching augmentation " + augmentation);
                    Map<String, QName> providedServices = findProvidedServices(
                            moduleIdentity, currentModule, qNamesToSIEs,
                            schemaContext);

                    if (moduleIdentity == null) {
                        throw new IllegalStateException(
                                "Cannot find identity specified by augmentation xpath constraint: "
                                        + moduleLocalNameFromXPath + " of "
                                        + augmentation);
                    }
                    String javaNamePrefix = findJavaNamePrefix(moduleIdentity);

                    Map<String, AttributeIfc> yangToAttributes = null;
                    // runtime-data
                    Collection<RuntimeBeanEntry> runtimeBeans = null;

                    if (expectedConfigurationAugmentationSchemaPath
                            .equals(augmentation.getTargetPath())) {
                        logger.debug("Parsing configuration of {}",
                                moduleLocalNameFromXPath);
                        yangToAttributes = fillConfiguration(choiceCaseNode,
                                currentModule, typeProviderWrapper,
                                qNamesToSIEs, schemaContext);
                        checkUniqueAttributesWithGeneratedClass(
                                uniqueGeneratedClassesNames, when.getQName(),
                                yangToAttributes);
                    } else if (expectedStateAugmentationSchemaPath
                            .equals(augmentation.getTargetPath())) {
                        logger.debug("Parsing state of {}",
                                moduleLocalNameFromXPath);
                        try {
                            runtimeBeans = fillRuntimeBeans(choiceCaseNode,
                                    currentModule, typeProviderWrapper,
                                    packageName, moduleLocalNameFromXPath,
                                    javaNamePrefix);
                        } catch (NameConflictException e) {
                            throw new NameConflictException(
                                    e.getConflictingName(), when.getQName(),
                                    when.getQName());
                        }

                        checkUniqueRuntimeBeansGeneratedClasses(
                                uniqueGeneratedClassesNames, when, runtimeBeans);
                        Set<RuntimeBeanEntry> runtimeBeanEntryValues = Sets
                                .newHashSet(runtimeBeans);
                        for (RuntimeBeanEntry entry : runtimeBeanEntryValues) {
                            checkUniqueAttributesWithGeneratedClass(
                                    uniqueGeneratedClassesNames,
                                    when.getQName(),
                                    entry.getYangPropertiesToTypesMap());
                        }

                    } else {
                        throw new IllegalArgumentException(
                                "Cannot parse augmentation " + augmentation);
                    }
                    if (result.containsKey(moduleLocalNameFromXPath)) {
                        // either fill runtimeBeans or yangToAttributes
                        ModuleMXBeanEntry moduleMXBeanEntry = result
                                .get(moduleLocalNameFromXPath);
                        if (yangToAttributes != null
                                && moduleMXBeanEntry.getAttributes() == null) {
                            moduleMXBeanEntry
                                    .setYangToAttributes(yangToAttributes);
                        } else if (runtimeBeans != null
                                && moduleMXBeanEntry.getRuntimeBeans() == null) {
                            moduleMXBeanEntry.setRuntimeBeans(runtimeBeans);
                        }
                    } else {
                        // construct ModuleMXBeanEntry
                        ModuleMXBeanEntry moduleMXBeanEntry = new ModuleMXBeanEntry(
                                moduleIdentity, yangToAttributes, packageName,
                                providedServices, javaNamePrefix, currentModule
                                        .getNamespace().toString(),
                                runtimeBeans);
                        moduleMXBeanEntry.setYangModuleName(currentModule
                                .getName());
                        moduleMXBeanEntry
                                .setYangModuleLocalname(moduleLocalNameFromXPath);
                        result.put(moduleLocalNameFromXPath, moduleMXBeanEntry);
                    }
                } // skip if child node is not ChoiceCaseNode
            } // skip if childNodes != 1
        }
        // clean up nulls
        for (Entry<String, ModuleMXBeanEntry> entry : result.entrySet()) {
            ModuleMXBeanEntry module = entry.getValue();
            if (module.getAttributes() == null) {
                module.setYangToAttributes(Collections
                        .<String, AttributeIfc> emptyMap());
            } else if (module.getRuntimeBeans() == null) {
                module.setRuntimeBeans(Collections
                        .<RuntimeBeanEntry> emptyList());
            }
        }
        if (unaugmentedModuleIdentities.size() > 0) {
            logger.warn("Augmentation not found for all module identities: {}",
                    unaugmentedModuleIdentities.keySet());
        }

        logger.debug("Number of ModuleMXBeans to be generated: {}",
                result.size());
        return result;
    }

    private static void checkUniqueRuntimeBeansGeneratedClasses(
            Map<String, QName> uniqueGeneratedClassesNames,
            DataSchemaNode when, Collection<RuntimeBeanEntry> runtimeBeans) {
        for (RuntimeBeanEntry runtimeBean : runtimeBeans) {
            final String javaNameOfRuntimeMXBean = runtimeBean
                    .getJavaNameOfRuntimeMXBean();
            if (uniqueGeneratedClassesNames
                    .containsKey(javaNameOfRuntimeMXBean)) {
                QName firstDefinedQName = uniqueGeneratedClassesNames
                        .get(javaNameOfRuntimeMXBean);
                throw new NameConflictException(javaNameOfRuntimeMXBean,
                        firstDefinedQName, when.getQName());
            }
            uniqueGeneratedClassesNames.put(javaNameOfRuntimeMXBean,
                    when.getQName());
        }
    }

    private static void checkUniqueAttributesWithGeneratedClass(
            Map<String, QName> uniqueGeneratedClassNames, QName parentQName,
            Map<String, AttributeIfc> yangToAttributes) {
        for (Entry<String, AttributeIfc> attr : yangToAttributes.entrySet()) {
            if (attr.getValue() instanceof TOAttribute) {
                checkUniqueTOAttr(uniqueGeneratedClassNames, parentQName,
                        (TOAttribute) attr.getValue());
            } else if (attr.getValue() instanceof ListAttribute
                    && ((ListAttribute) attr.getValue()).getInnerAttribute() instanceof TOAttribute) {
                checkUniqueTOAttr(uniqueGeneratedClassNames, parentQName,
                        (TOAttribute) ((ListAttribute) attr.getValue())
                                .getInnerAttribute());
            }
        }
    }

    private static void checkUniqueTOAttr(
            Map<String, QName> uniqueGeneratedClassNames, QName parentQName,
            TOAttribute attr) {
        final String upperCaseCammelCase = attr.getUpperCaseCammelCase();
        if (uniqueGeneratedClassNames.containsKey(upperCaseCammelCase)) {
            QName firstDefinedQName = uniqueGeneratedClassNames
                    .get(upperCaseCammelCase);
            throw new NameConflictException(upperCaseCammelCase,
                    firstDefinedQName, parentQName);
        } else {
            uniqueGeneratedClassNames.put(upperCaseCammelCase, parentQName);
        }
    }

    private static Collection<RuntimeBeanEntry> fillRuntimeBeans(
            ChoiceCaseNode choiceCaseNode, Module currentModule,
            TypeProviderWrapper typeProviderWrapper, String packageName,
            String moduleLocalNameFromXPath, String javaNamePrefix) {

        return RuntimeBeanEntry.extractClassNameToRuntimeBeanMap(packageName,
                choiceCaseNode, moduleLocalNameFromXPath, typeProviderWrapper,
                javaNamePrefix, currentModule).values();

    }

    private static Map<String, AttributeIfc> fillConfiguration(
            ChoiceCaseNode choiceCaseNode, Module currentModule,
            TypeProviderWrapper typeProviderWrapper,
            Map<QName, ServiceInterfaceEntry> qNamesToSIEs,
            SchemaContext schemaContext) {
        Map<String, AttributeIfc> yangToAttributes = new HashMap<>();
        for (DataSchemaNode attrNode : choiceCaseNode.getChildNodes()) {
            AttributeIfc attributeValue = getAttributeValue(attrNode,
                    currentModule, qNamesToSIEs, typeProviderWrapper,
                    schemaContext);
            yangToAttributes.put(attributeValue.getAttributeYangName(),
                    attributeValue);
        }
        return yangToAttributes;
    }

    private static Map<String, QName> findProvidedServices(
            IdentitySchemaNode moduleIdentity, Module currentModule,
            Map<QName, ServiceInterfaceEntry> qNamesToSIEs,
            SchemaContext schemaContext) {
        Map<String, QName> result = new HashMap<>();
        for (UnknownSchemaNode unknownNode : moduleIdentity
                .getUnknownSchemaNodes()) {
            if (ConfigConstants.PROVIDED_SERVICE_EXTENSION_QNAME
                    .equals(unknownNode.getNodeType())) {
                String prefixAndIdentityLocalName = unknownNode
                        .getNodeParameter();
                ServiceInterfaceEntry sie = findSIE(prefixAndIdentityLocalName,
                        currentModule, qNamesToSIEs, schemaContext);
                result.put(sie.getFullyQualifiedName(), sie.getQName());
            }
        }
        return result;
    }

    /**
     * For input node, find if it contains config:java-name-prefix extension. If
     * not found, convert local name of node converted to cammel case.
     */
    public static String findJavaNamePrefix(SchemaNode schemaNode) {
        return convertToJavaName(schemaNode, true);
    }

    public static String findJavaParameter(SchemaNode schemaNode) {
        return convertToJavaName(schemaNode, false);
    }

    public static String convertToJavaName(SchemaNode schemaNode,
            boolean capitalizeFirstLetter) {
        for (UnknownSchemaNode unknownNode : schemaNode.getUnknownSchemaNodes()) {
            if (ConfigConstants.JAVA_NAME_PREFIX_EXTENSION_QNAME
                    .equals(unknownNode.getNodeType())) {
                String value = unknownNode.getNodeParameter();
                return convertToJavaName(value, capitalizeFirstLetter);
            }
        }
        return convertToJavaName(schemaNode.getQName().getLocalName(),
                capitalizeFirstLetter);
    }

    public static String convertToJavaName(String localName,
            boolean capitalizeFirstLetter) {
        if (capitalizeFirstLetter) {
            return BindingGeneratorUtil.parseToClassName(localName);
        } else {
            return BindingGeneratorUtil.parseToValidParamName(localName);
        }
    }

    private static int getChildNodeSizeWithoutUses(ContainerSchemaNode csn) {
        int result = 0;
        for (DataSchemaNode dsn : csn.getChildNodes()) {
            if (dsn.isAddedByUses() == false)
                result++;
        }
        return result;
    }

    private static AttributeIfc getAttributeValue(DataSchemaNode attrNode,
            Module currentModule,
            Map<QName, ServiceInterfaceEntry> qNamesToSIEs,
            TypeProviderWrapper typeProviderWrapper, SchemaContext schemaContext) {

        if (attrNode instanceof LeafSchemaNode) {
            // simple type
            LeafSchemaNode leaf = (LeafSchemaNode) attrNode;
            return new JavaAttribute(leaf, typeProviderWrapper);
        } else if (attrNode instanceof ContainerSchemaNode) {
            // reference or TO
            ContainerSchemaNode containerSchemaNode = (ContainerSchemaNode) attrNode;
            if (containerSchemaNode.getUses().size() == 1
                    && getChildNodeSizeWithoutUses(containerSchemaNode) == 0) {
                // reference
                UsesNode usesNode = containerSchemaNode.getUses().iterator()
                        .next();
                checkState(usesNode.getRefines().size() == 1,
                        "Unexpected 'refine' child node size of "
                                + containerSchemaNode);
                LeafSchemaNode refine = (LeafSchemaNode) usesNode.getRefines()
                        .values().iterator().next();
                checkState(refine.getUnknownSchemaNodes().size() == 1,
                        "Unexpected unknown schema node size of " + refine);
                UnknownSchemaNode requiredIdentity = refine
                        .getUnknownSchemaNodes().iterator().next();
                checkState(
                        ConfigConstants.REQUIRED_IDENTITY_EXTENSION_QNAME.equals(requiredIdentity
                                .getNodeType()),
                        "Unexpected language extension " + requiredIdentity);
                String prefixAndIdentityLocalName = requiredIdentity
                        .getNodeParameter();
                // import should point to a module
                ServiceInterfaceEntry serviceInterfaceEntry = findSIE(
                        prefixAndIdentityLocalName, currentModule,
                        qNamesToSIEs, schemaContext);
                boolean mandatory = refine.getConstraints().isMandatory();
                return new DependencyAttribute(attrNode, serviceInterfaceEntry,
                        mandatory, attrNode.getDescription());
            } else {
                return TOAttribute.create(containerSchemaNode,
                        typeProviderWrapper);
            }
        } else if (attrNode instanceof LeafListSchemaNode) {
            return ListAttribute.create((LeafListSchemaNode) attrNode,
                    typeProviderWrapper);
        } else if (attrNode instanceof ListSchemaNode) {
            return ListAttribute.create((ListSchemaNode) attrNode,
                    typeProviderWrapper);
        } else {
            throw new UnsupportedOperationException(
                    "Unknown configuration node " + attrNode.toString());
        }
    }

    private static ServiceInterfaceEntry findSIE(
            String prefixAndIdentityLocalName, Module currentModule,
            Map<QName, ServiceInterfaceEntry> qNamesToSIEs,
            SchemaContext schemaContext) {

        Matcher m = PREFIX_COLON_LOCAL_NAME.matcher(prefixAndIdentityLocalName);
        Module foundModule;
        String localSIName;
        if (m.matches()) {
            // if there is a prefix, look for ModuleImport with this prefix. Get
            // Module from SchemaContext
            String prefix = m.group(1);
            ModuleImport moduleImport = findModuleImport(currentModule, prefix);
            foundModule = schemaContext.findModuleByName(
                    moduleImport.getModuleName(), moduleImport.getRevision());
            checkState(
                    foundModule != null,
                    format("Module not found in SchemaContext by %s",
                            moduleImport));
            localSIName = m.group(2);
        } else {
            foundModule = currentModule; // no prefix => SIE is in currentModule
            localSIName = prefixAndIdentityLocalName;
        }
        QName siQName = new QName(foundModule.getNamespace(),
                foundModule.getRevision(), localSIName);
        ServiceInterfaceEntry sie = qNamesToSIEs.get(siQName);
        checkState(sie != null, "Cannot find referenced Service Interface by "
                + prefixAndIdentityLocalName);
        return sie;
    }

    private static ModuleImport findModuleImport(Module module, String prefix) {
        for (ModuleImport moduleImport : module.getImports()) {
            if (moduleImport.getPrefix().equals(prefix)) {
                return moduleImport;
            }
        }
        throw new IllegalStateException(format(
                "Import not found with prefix %s in %s", prefix, module));
    }

    public Map<String, AttributeIfc> getAttributes() {
        return yangToAttributes;
    }

    private void setYangToAttributes(Map<String, AttributeIfc> newAttributes) {
        this.yangToAttributes = newAttributes;

    }

    public String getNullableDescription() {
        return nullableDescription;
    }

    @Override
    public String toString() {
        return "ModuleMXBeanEntry{" + "globallyUniqueName='"
                + globallyUniqueName + '\'' + ", packageName='" + packageName
                + '\'' + '}';
    }
}
