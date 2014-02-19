/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AbstractDependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListDependenciesAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.NameConflictException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleImport;
import org.opendaylight.yangtools.yang.model.api.RevisionAwareXPath;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.opendaylight.controller.config.yangjmxgenerator.ConfigConstants.createConfigQName;

final class ModuleMXBeanEntryBuilder {

    private Module currentModule;
    private Map<QName, ServiceInterfaceEntry> qNamesToSIEs;
    private SchemaContext schemaContext;
    private TypeProviderWrapper typeProviderWrapper;
    private String packageName;

    public ModuleMXBeanEntryBuilder setModule(Module module) {
        this.currentModule = module;
        return this;
    }

    public ModuleMXBeanEntryBuilder setqNamesToSIEs(Map<QName, ServiceInterfaceEntry> qNamesToSIEs) {
        this.qNamesToSIEs = qNamesToSIEs;
        return this;
    }

    public ModuleMXBeanEntryBuilder setSchemaContext(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
        return this;
    }

    public ModuleMXBeanEntryBuilder setTypeProviderWrapper(TypeProviderWrapper typeProviderWrapper) {
        this.typeProviderWrapper = typeProviderWrapper;
        return this;
    }

    public ModuleMXBeanEntryBuilder setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    private static final Logger logger = LoggerFactory
            .getLogger(ModuleMXBeanEntryBuilder.class);

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


    public Map<String, ModuleMXBeanEntry> build() {
        logger.debug("Generating ModuleMXBeans of {} to package {}",
                currentModule.getNamespace(), packageName);

        String configModulePrefix;
        try {
            configModulePrefix = getConfigModulePrefixFromImport(currentModule);
        } catch (IllegalArgumentException e) {
            // this currentModule does not import config currentModule
            return Collections.emptyMap();
        }

        // get identities of base config:currentModule-type
        Map<String, IdentitySchemaNode> moduleIdentities =  getIdentityMap();

        Map<String, QName> uniqueGeneratedClassesNames = new HashMap<>();

        // each currentModule name should have an augmentation defined
        Map<String, IdentitySchemaNode> unaugmentedModuleIdentities = new HashMap<>(
                moduleIdentities);

        Map<String, ModuleMXBeanEntry> result = new HashMap<>();

        for (AugmentationSchema augmentation : currentModule.getAugmentations()) {
            Set<DataSchemaNode> childNodes = augmentation.getChildNodes();
            if (areAllChildrenChoiceCaseNodes(childNodes)) {
                for (ChoiceCaseNode childCase : castChildNodesToChoiceCases(childNodes)) {
                    // TODO refactor, extract to standalone builder class
                    processChoiceCaseNode(result, uniqueGeneratedClassesNames, configModulePrefix, moduleIdentities,
                            unaugmentedModuleIdentities, augmentation, childCase);
                }
            } // skip if child nodes are not all cases
        }
        // clean up nulls
        cleanUpNulls(result);
        // check attributes name uniqueness
        checkAttributeNamesUniqueness(uniqueGeneratedClassesNames, result);
        checkUnaugumentedIdentities(unaugmentedModuleIdentities);

        logger.debug("Number of ModuleMXBeans to be generated: {}", result.size());

        return result;
    }

    private static void cleanUpNulls(Map<String, ModuleMXBeanEntry> result) {
        for (Map.Entry<String, ModuleMXBeanEntry> entry : result.entrySet()) {
            ModuleMXBeanEntry module = entry.getValue();
            if (module.getAttributes() == null) {
                module.setYangToAttributes(Collections
                        .<String, AttributeIfc> emptyMap());
            } else if (module.getRuntimeBeans() == null) {
                module.setRuntimeBeans(Collections
                        .<RuntimeBeanEntry> emptyList());
            }
        }
    }

    private static void checkUnaugumentedIdentities(Map<String, IdentitySchemaNode> unaugmentedModuleIdentities) {
        if (unaugmentedModuleIdentities.size() > 0) {
            logger.warn("Augmentation not found for all currentModule identities: {}",
                    unaugmentedModuleIdentities.keySet());
        }
    }

    private static void checkAttributeNamesUniqueness(Map<String, QName> uniqueGeneratedClassesNames, Map<String, ModuleMXBeanEntry> result) {
        for (Map.Entry<String, ModuleMXBeanEntry> entry : result.entrySet()) {
            checkUniqueRuntimeBeanAttributesName(entry.getValue(),
                    uniqueGeneratedClassesNames);
        }
    }

    private Map<String, IdentitySchemaNode> getIdentityMap() {
        Map<String, IdentitySchemaNode> moduleIdentities = Maps.newHashMap();

        for (IdentitySchemaNode id : currentModule.getIdentities()) {
            if (id.getBaseIdentity() != null
                    && ConfigConstants.MODULE_TYPE_Q_NAME.equals(id.getBaseIdentity().getQName())) {
                String identityLocalName = id.getQName().getLocalName();
                if (moduleIdentities.containsKey(identityLocalName)) {
                    throw new IllegalStateException("Module name already defined in this currentModule: "
                            + identityLocalName);
                } else {
                    moduleIdentities.put(identityLocalName, id);
                    logger.debug("Found identity {}", identityLocalName);
                }
                // validation check on unknown schema nodes
                boolean providedServiceWasSet = false;
                for (UnknownSchemaNode unknownNode : id.getUnknownSchemaNodes()) {
                    // TODO: test this
                    boolean unknownNodeIsProvidedServiceExtension = ConfigConstants.PROVIDED_SERVICE_EXTENSION_QNAME.equals(unknownNode.getNodeType());
                    // true => no op: 0 or more provided identities are allowed

                    if (ConfigConstants.JAVA_NAME_PREFIX_EXTENSION_QNAME.equals(unknownNode.getNodeType())) {
                        // 0..1 allowed
                        checkState(
                                providedServiceWasSet == false,
                                format("More than one language extension %s is not allowed here: %s",
                                        ConfigConstants.JAVA_NAME_PREFIX_EXTENSION_QNAME, id));
                        providedServiceWasSet = true;
                    } else if (unknownNodeIsProvidedServiceExtension == false) {
                        throw new IllegalStateException("Unexpected language extension " + unknownNode.getNodeType());
                    }
                }
            }
        }

        return moduleIdentities;
    }

    private Collection<ChoiceCaseNode> castChildNodesToChoiceCases(Set<DataSchemaNode> childNodes) {
        return Collections2.transform(childNodes, new Function<DataSchemaNode, ChoiceCaseNode>() {
            @Nullable
            @Override
            public ChoiceCaseNode apply(@Nullable DataSchemaNode input) {
                return (ChoiceCaseNode) input;
            }
        });
    }

    private boolean areAllChildrenChoiceCaseNodes(Set<DataSchemaNode> childNodes) {
        for (DataSchemaNode childNode : childNodes) {
            if (childNode instanceof ChoiceCaseNode == false)
                return false;
        }
        return true;
    }

    private void processChoiceCaseNode(Map<String, ModuleMXBeanEntry> result,
            Map<String, QName> uniqueGeneratedClassesNames, String configModulePrefix,
            Map<String, IdentitySchemaNode> moduleIdentities,
            Map<String, IdentitySchemaNode> unaugmentedModuleIdentities, AugmentationSchema augmentation,
            DataSchemaNode when) {

        ChoiceCaseNode choiceCaseNode = (ChoiceCaseNode) when;
        if (choiceCaseNode.getConstraints() == null || choiceCaseNode.getConstraints().getWhenCondition() == null) {
            return;
        }
        RevisionAwareXPath xPath = choiceCaseNode.getConstraints().getWhenCondition();
        Matcher matcher = getWhenConditionMatcher(configModulePrefix, xPath);
        if (matcher.matches() == false) {
            return;
        }
        String moduleLocalNameFromXPath = matcher.group(1);
        IdentitySchemaNode moduleIdentity = moduleIdentities.get(moduleLocalNameFromXPath);
        unaugmentedModuleIdentities.remove(moduleLocalNameFromXPath);
        checkState(moduleIdentity != null, "Cannot find identity " + moduleLocalNameFromXPath
                + " matching augmentation " + augmentation);
        Map<String, QName> providedServices = findProvidedServices(moduleIdentity, currentModule, qNamesToSIEs,
                schemaContext);

        if (moduleIdentity == null) {
            throw new IllegalStateException("Cannot find identity specified by augmentation xpath constraint: "
                    + moduleLocalNameFromXPath + " of " + augmentation);
        }
        String javaNamePrefix = TypeProviderWrapper.findJavaNamePrefix(moduleIdentity);

        Map<String, AttributeIfc> yangToAttributes = null;
        // runtime-data
        Collection<RuntimeBeanEntry> runtimeBeans = null;

        if (expectedConfigurationAugmentationSchemaPath.equals(augmentation.getTargetPath())) {
            logger.debug("Parsing configuration of {}", moduleLocalNameFromXPath);
            yangToAttributes = fillConfiguration(choiceCaseNode, currentModule, typeProviderWrapper, qNamesToSIEs,
                    schemaContext, packageName);
            checkUniqueAttributesWithGeneratedClass(uniqueGeneratedClassesNames, when.getQName(), yangToAttributes);
        } else if (expectedStateAugmentationSchemaPath.equals(augmentation.getTargetPath())) {
            logger.debug("Parsing state of {}", moduleLocalNameFromXPath);
            try {
                runtimeBeans = fillRuntimeBeans(choiceCaseNode, currentModule, typeProviderWrapper, packageName,
                        moduleLocalNameFromXPath, javaNamePrefix);
            } catch (NameConflictException e) {
                throw new NameConflictException(e.getConflictingName(), when.getQName(), when.getQName());
            }
            checkUniqueRuntimeBeansGeneratedClasses(uniqueGeneratedClassesNames, when, runtimeBeans);
            Set<RuntimeBeanEntry> runtimeBeanEntryValues = Sets.newHashSet(runtimeBeans);
            for (RuntimeBeanEntry entry : runtimeBeanEntryValues) {
                checkUniqueAttributesWithGeneratedClass(uniqueGeneratedClassesNames, when.getQName(),
                        entry.getYangPropertiesToTypesMap());
            }

        } else {
            throw new IllegalArgumentException("Cannot parse augmentation " + augmentation);
        }
        if (result.containsKey(moduleLocalNameFromXPath)) {
            // either fill runtimeBeans or yangToAttributes
            ModuleMXBeanEntry moduleMXBeanEntry = result.get(moduleLocalNameFromXPath);
            if (yangToAttributes != null && moduleMXBeanEntry.getAttributes() == null) {
                moduleMXBeanEntry.setYangToAttributes(yangToAttributes);
            } else if (runtimeBeans != null && moduleMXBeanEntry.getRuntimeBeans() == null) {
                moduleMXBeanEntry.setRuntimeBeans(runtimeBeans);
            }
        } else {
            ModuleMXBeanEntry.ModuleMXBeanEntryInitial initial = new ModuleMXBeanEntry.ModuleMXBeanEntryInitialBuilder()
                    .setIdSchemaNode(moduleIdentity).setPackageName(packageName).setJavaNamePrefix(javaNamePrefix)
                    .setNamespace(currentModule.getNamespace().toString()).setqName(ModuleUtil.getQName(currentModule))
                    .build();

            // construct ModuleMXBeanEntry
            ModuleMXBeanEntry moduleMXBeanEntry = new ModuleMXBeanEntry(initial, yangToAttributes, providedServices,
                    runtimeBeans);

            moduleMXBeanEntry.setYangModuleName(currentModule.getName());
            moduleMXBeanEntry.setYangModuleLocalname(moduleLocalNameFromXPath);
            result.put(moduleLocalNameFromXPath, moduleMXBeanEntry);
        }
    }

    private void checkUniqueRuntimeBeansGeneratedClasses(Map<String, QName> uniqueGeneratedClassesNames,
            DataSchemaNode when, Collection<RuntimeBeanEntry> runtimeBeans) {
        for (RuntimeBeanEntry runtimeBean : runtimeBeans) {
            final String javaNameOfRuntimeMXBean = runtimeBean.getJavaNameOfRuntimeMXBean();
            if (uniqueGeneratedClassesNames.containsKey(javaNameOfRuntimeMXBean)) {
                QName firstDefinedQName = uniqueGeneratedClassesNames.get(javaNameOfRuntimeMXBean);
                throw new NameConflictException(javaNameOfRuntimeMXBean, firstDefinedQName, when.getQName());
            }
            uniqueGeneratedClassesNames.put(javaNameOfRuntimeMXBean, when.getQName());
        }
    }

    private static void checkUniqueRuntimeBeanAttributesName(ModuleMXBeanEntry mxBeanEntry,
            Map<String, QName> uniqueGeneratedClassesNames) {
        for (RuntimeBeanEntry runtimeBeanEntry : mxBeanEntry.getRuntimeBeans()) {
            for (String runtimeAttName : runtimeBeanEntry.getYangPropertiesToTypesMap().keySet()) {
                if (mxBeanEntry.getAttributes().keySet().contains(runtimeAttName)) {
                    QName qName1 = uniqueGeneratedClassesNames.get(runtimeBeanEntry.getJavaNameOfRuntimeMXBean());
                    QName qName2 = uniqueGeneratedClassesNames.get(mxBeanEntry.getGloballyUniqueName());
                    throw new NameConflictException(runtimeAttName, qName1, qName2);
                }
            }
        }
    }

    private void checkUniqueAttributesWithGeneratedClass(Map<String, QName> uniqueGeneratedClassNames,
            QName parentQName, Map<String, AttributeIfc> yangToAttributes) {
        for (Map.Entry<String, AttributeIfc> attr : yangToAttributes.entrySet()) {
            if (attr.getValue() instanceof TOAttribute) {
                checkUniqueTOAttr(uniqueGeneratedClassNames, parentQName, (TOAttribute) attr.getValue());
            } else if (attr.getValue() instanceof ListAttribute
                    && ((ListAttribute) attr.getValue()).getInnerAttribute() instanceof TOAttribute) {
                checkUniqueTOAttr(uniqueGeneratedClassNames, parentQName,
                        (TOAttribute) ((ListAttribute) attr.getValue()).getInnerAttribute());
            }
        }
    }

    private void checkUniqueTOAttr(Map<String, QName> uniqueGeneratedClassNames, QName parentQName, TOAttribute attr) {
        final String upperCaseCammelCase = attr.getUpperCaseCammelCase();
        if (uniqueGeneratedClassNames.containsKey(upperCaseCammelCase)) {
            QName firstDefinedQName = uniqueGeneratedClassNames.get(upperCaseCammelCase);
            throw new NameConflictException(upperCaseCammelCase, firstDefinedQName, parentQName);
        } else {
            uniqueGeneratedClassNames.put(upperCaseCammelCase, parentQName);
        }
    }

    private Collection<RuntimeBeanEntry> fillRuntimeBeans(ChoiceCaseNode choiceCaseNode, Module currentModule,
            TypeProviderWrapper typeProviderWrapper, String packageName, String moduleLocalNameFromXPath,
            String javaNamePrefix) {

        return RuntimeBeanEntry.extractClassNameToRuntimeBeanMap(packageName, choiceCaseNode, moduleLocalNameFromXPath,
                typeProviderWrapper, javaNamePrefix, currentModule).values();

    }

    private Map<String, AttributeIfc> fillConfiguration(ChoiceCaseNode choiceCaseNode, Module currentModule,
            TypeProviderWrapper typeProviderWrapper, Map<QName, ServiceInterfaceEntry> qNamesToSIEs,
            SchemaContext schemaContext, String packageName) {
        Map<String, AttributeIfc> yangToAttributes = new HashMap<>();
        for (DataSchemaNode attrNode : choiceCaseNode.getChildNodes()) {
            AttributeIfc attributeValue = getAttributeValue(attrNode, currentModule, qNamesToSIEs, typeProviderWrapper,
                    schemaContext, packageName);
            yangToAttributes.put(attributeValue.getAttributeYangName(), attributeValue);
        }
        return yangToAttributes;
    }

    private Map<String, QName> findProvidedServices(IdentitySchemaNode moduleIdentity, Module currentModule,
            Map<QName, ServiceInterfaceEntry> qNamesToSIEs, SchemaContext schemaContext) {
        Map<String, QName> result = new HashMap<>();
        for (UnknownSchemaNode unknownNode : moduleIdentity.getUnknownSchemaNodes()) {
            if (ConfigConstants.PROVIDED_SERVICE_EXTENSION_QNAME.equals(unknownNode.getNodeType())) {
                String prefixAndIdentityLocalName = unknownNode.getNodeParameter();
                ServiceInterfaceEntry sie = findSIE(prefixAndIdentityLocalName, currentModule, qNamesToSIEs,
                        schemaContext);
                result.put(sie.getFullyQualifiedName(), sie.getQName());
            }
        }
        return result;
    }

    private AttributeIfc getAttributeValue(DataSchemaNode attrNode, Module currentModule,
            Map<QName, ServiceInterfaceEntry> qNamesToSIEs, TypeProviderWrapper typeProviderWrapper,
            SchemaContext schemaContext, String packageName) {

        if (attrNode instanceof LeafSchemaNode) {
            // simple type
            LeafSchemaNode leaf = (LeafSchemaNode) attrNode;
            return new JavaAttribute(leaf, typeProviderWrapper);
        } else if (attrNode instanceof ContainerSchemaNode) {
            // reference or TO
            ContainerSchemaNode containerSchemaNode = (ContainerSchemaNode) attrNode;
            Optional<? extends AbstractDependencyAttribute> dependencyAttributeOptional = extractDependency(
                    containerSchemaNode, attrNode, currentModule, qNamesToSIEs, schemaContext);
            if (dependencyAttributeOptional.isPresent()) {
                return dependencyAttributeOptional.get();
            } else {
                return TOAttribute.create(containerSchemaNode, typeProviderWrapper, packageName);
            }

        } else if (attrNode instanceof LeafListSchemaNode) {
            return ListAttribute.create((LeafListSchemaNode) attrNode, typeProviderWrapper);
        } else if (attrNode instanceof ListSchemaNode) {
            ListSchemaNode listSchemaNode = (ListSchemaNode) attrNode;
            Optional<? extends AbstractDependencyAttribute> dependencyAttributeOptional = extractDependency(
                    listSchemaNode, attrNode, currentModule, qNamesToSIEs, schemaContext);
            if (dependencyAttributeOptional.isPresent()) {
                return dependencyAttributeOptional.get();
            } else {
                return ListAttribute.create(listSchemaNode, typeProviderWrapper, packageName);
            }
        } else {
            throw new UnsupportedOperationException("Unknown configuration node " + attrNode.toString());
        }
    }

    private Optional<? extends AbstractDependencyAttribute> extractDependency(DataNodeContainer dataNodeContainer,
            DataSchemaNode attrNode, Module currentModule, Map<QName, ServiceInterfaceEntry> qNamesToSIEs,
            SchemaContext schemaContext) {
        if (dataNodeContainer.getUses().size() == 1 && getChildNodeSizeWithoutUses(dataNodeContainer) == 0) {
            // reference
            UsesNode usesNode = dataNodeContainer.getUses().iterator().next();
            checkState(usesNode.getRefines().size() == 1, "Unexpected 'refine' child node size of " + dataNodeContainer);
            LeafSchemaNode refine = (LeafSchemaNode) usesNode.getRefines().values().iterator().next();
            checkState(refine.getUnknownSchemaNodes().size() == 1, "Unexpected unknown schema node size of " + refine);
            UnknownSchemaNode requiredIdentity = refine.getUnknownSchemaNodes().iterator().next();
            checkState(ConfigConstants.REQUIRED_IDENTITY_EXTENSION_QNAME.equals(requiredIdentity.getNodeType()),
                    "Unexpected language extension " + requiredIdentity);
            String prefixAndIdentityLocalName = requiredIdentity.getNodeParameter();
            // import should point to a module
            ServiceInterfaceEntry serviceInterfaceEntry = findSIE(prefixAndIdentityLocalName, currentModule,
                    qNamesToSIEs, schemaContext);
            boolean mandatory = refine.getConstraints().isMandatory();
            AbstractDependencyAttribute reference;
            if (dataNodeContainer instanceof ContainerSchemaNode) {
                reference = new DependencyAttribute(attrNode, serviceInterfaceEntry, mandatory,
                        attrNode.getDescription());
            } else {
                reference = new ListDependenciesAttribute(attrNode, serviceInterfaceEntry, mandatory,
                        attrNode.getDescription());
            }
            return Optional.of(reference);
        }
        return Optional.absent();
    }

    private int getChildNodeSizeWithoutUses(DataNodeContainer csn) {
        int result = 0;
        for (DataSchemaNode dsn : csn.getChildNodes()) {
            if (dsn.isAddedByUses() == false) {
                result++;
            }
        }
        return result;
    }

    private ServiceInterfaceEntry findSIE(String prefixAndIdentityLocalName, Module currentModule,
            Map<QName, ServiceInterfaceEntry> qNamesToSIEs, SchemaContext schemaContext) {

        Matcher m = PREFIX_COLON_LOCAL_NAME.matcher(prefixAndIdentityLocalName);
        Module foundModule;
        String localSIName;
        if (m.matches()) {
            // if there is a prefix, look for ModuleImport with this prefix. Get
            // Module from SchemaContext
            String prefix = m.group(1);
            ModuleImport moduleImport = findModuleImport(currentModule, prefix);
            foundModule = schemaContext.findModuleByName(moduleImport.getModuleName(), moduleImport.getRevision());
            checkState(foundModule != null, format("Module not found in SchemaContext by %s", moduleImport));
            localSIName = m.group(2);
        } else {
            foundModule = currentModule; // no prefix => SIE is in currentModule
            localSIName = prefixAndIdentityLocalName;
        }
        QName siQName = new QName(foundModule.getNamespace(), foundModule.getRevision(), localSIName);
        ServiceInterfaceEntry sie = qNamesToSIEs.get(siQName);
        checkState(sie != null, "Cannot find referenced Service Interface by " + prefixAndIdentityLocalName);
        return sie;
    }

    private ModuleImport findModuleImport(Module module, String prefix) {
        for (ModuleImport moduleImport : module.getImports()) {
            if (moduleImport.getPrefix().equals(prefix)) {
                return moduleImport;
            }
        }
        throw new IllegalStateException(format("Import not found with prefix %s in %s", prefix, module));
    }

    @VisibleForTesting
    static Matcher getWhenConditionMatcher(String prefix, RevisionAwareXPath whenConstraint) {
        String xpathRegex = MODULE_CONDITION_XPATH_TEMPLATE.replace(MAGIC_STRING, prefix);
        Pattern pattern = Pattern.compile(xpathRegex);
        return pattern.matcher(whenConstraint.toString());
    }

    String getConfigModulePrefixFromImport(Module currentModule) {
        for (ModuleImport currentImport : currentModule.getImports()) {
            if (currentImport.getModuleName().equals(ConfigConstants.CONFIG_MODULE)) {
                return currentImport.getPrefix();
            }
        }
        throw new IllegalArgumentException("Cannot find import " + ConfigConstants.CONFIG_MODULE + " in "
                + currentModule);
    }

}
