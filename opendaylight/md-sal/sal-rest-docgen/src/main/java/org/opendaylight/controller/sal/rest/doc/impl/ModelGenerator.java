/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.BooleanUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.controller.sal.rest.doc.model.builder.OperationBuilder;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ConstraintDefinition;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BinaryTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition.Bit;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LengthConstraint;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.BooleanType;
import org.opendaylight.yangtools.yang.model.util.Decimal64;
import org.opendaylight.yangtools.yang.model.util.EnumerationType;
import org.opendaylight.yangtools.yang.model.util.ExtendedType;
import org.opendaylight.yangtools.yang.model.util.Int16;
import org.opendaylight.yangtools.yang.model.util.Int32;
import org.opendaylight.yangtools.yang.model.util.Int64;
import org.opendaylight.yangtools.yang.model.util.Int8;
import org.opendaylight.yangtools.yang.model.util.StringType;
import org.opendaylight.yangtools.yang.model.util.Uint16;
import org.opendaylight.yangtools.yang.model.util.Uint32;
import org.opendaylight.yangtools.yang.model.util.Uint64;
import org.opendaylight.yangtools.yang.model.util.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates JSON Schema for data defined in Yang
 */
public class ModelGenerator {

    private static Logger _logger = LoggerFactory.getLogger(ModelGenerator.class);

    private static final String BASE_64 = "base64";
    private static final String BINARY_ENCODING_KEY = "binaryEncoding";
    private static final String MEDIA_KEY = "media";
    private static final String ONE_OF_KEY = "oneOf";
    private static final String UNIQUE_ITEMS_KEY = "uniqueItems";
    private static final String MAX_ITEMS = "maxItems";
    private static final String MIN_ITEMS = "minItems";
    private static final String SCHEMA_URL = "http://json-schema.org/draft-04/schema";
    private static final String SCHEMA_KEY = "$schema";
    private static final String MAX_LENGTH_KEY = "maxLength";
    private static final String MIN_LENGTH_KEY = "minLength";
    private static final String REQUIRED_KEY = "required";
    private static final String REF_KEY = "$ref";
    private static final String ITEMS_KEY = "items";
    private static final String TYPE_KEY = "type";
    private static final String PROPERTIES_KEY = "properties";
    private static final String DESCRIPTION_KEY = "description";
    private static final String OBJECT_TYPE = "object";
    private static final String ARRAY_TYPE = "array";
    private static final String ENUM = "enum";
    private static final String INTEGER = "integer";
    private static final String NUMBER = "number";
    private static final String BOOLEAN = "boolean";
    private static final String STRING = "string";
  private static final String ID_KEY = "id";
  private static final String SUB_TYPES_KEY = "subTypes";

    private static final Map<Class<? extends TypeDefinition<?>>, String> YANG_TYPE_TO_JSON_TYPE_MAPPING;

    static {
        Map<Class<? extends TypeDefinition<?>>, String> tempMap1 = new HashMap<Class<? extends TypeDefinition<?>>, String>(
                10);
        tempMap1.put(StringType.class, STRING);
        tempMap1.put(BooleanType.class, BOOLEAN);
        tempMap1.put(Int8.class, INTEGER);
        tempMap1.put(Int16.class, INTEGER);
        tempMap1.put(Int32.class, INTEGER);
        tempMap1.put(Int64.class, INTEGER);
        tempMap1.put(Uint16.class, INTEGER);
        tempMap1.put(Uint32.class, INTEGER);
        tempMap1.put(Uint64.class, INTEGER);
        tempMap1.put(Uint8.class, INTEGER);
        tempMap1.put(Decimal64.class, NUMBER);
        tempMap1.put(EnumerationType.class, ENUM);
        // TODO: Binary type

        YANG_TYPE_TO_JSON_TYPE_MAPPING = Collections.unmodifiableMap(tempMap1);
    }

    public ModelGenerator() {
    }

    public JSONObject convertToJsonSchema(Module module) throws IOException, JSONException {
        JSONObject models = new JSONObject();
        processContainers(module, models);
        processRPCs(module, models);
    processIdentities(module, models);
        return models;
    }

    private void processContainers(Module module, JSONObject models) throws IOException,
            JSONException {

        String moduleName = module.getName();
        Set<DataSchemaNode> childNodes = module.getChildNodes();

        for (DataSchemaNode childNode : childNodes) {
            JSONObject configModuleJSON = null;
            JSONObject operationalModuleJSON = null;

            String childNodeName = childNode.getQName().getLocalName();
            /*
             * For every container in the module
             */
            if (childNode instanceof ContainerSchemaNode) {
                configModuleJSON = processContainer((ContainerSchemaNode) childNode, moduleName,
                        true, models, true);
                operationalModuleJSON = processContainer((ContainerSchemaNode) childNode,
                        moduleName, true, models, false);
            }

            if (configModuleJSON != null) {
                _logger.debug("Adding model for [{}]", OperationBuilder.CONFIG + childNodeName);
                configModuleJSON.put("id", OperationBuilder.CONFIG + childNodeName);
                models.put(OperationBuilder.CONFIG + childNodeName, configModuleJSON);
            }
            if (operationalModuleJSON != null) {
                _logger.debug("Adding model for [{}]", OperationBuilder.OPERATIONAL + childNodeName);
                operationalModuleJSON.put("id", OperationBuilder.OPERATIONAL + childNodeName);
                models.put(OperationBuilder.OPERATIONAL + childNodeName, operationalModuleJSON);
            }
        }

    }

    /**
     * Process the RPCs for a Module Spits out a file each of the name
     * <rpcName>-input.json and <rpcName>-output.json for each RPC that contains
     * input & output elements
     *
     * @param module
     * @throws JSONException
     * @throws IOException
     */
    private void processRPCs(Module module, JSONObject models) throws JSONException, IOException {

        Set<RpcDefinition> rpcs = module.getRpcs();
        String moduleName = module.getName();
        for (RpcDefinition rpc : rpcs) {

            ContainerSchemaNode input = rpc.getInput();
            if (input != null) {
                JSONObject inputJSON = processContainer(input, moduleName, true, models);
                String filename = "(" + rpc.getQName().getLocalName() + ")input";
                inputJSON.put("id", filename);
                // writeToFile(filename, inputJSON.toString(2), moduleName);
                models.put(filename, inputJSON);
            }

            ContainerSchemaNode output = rpc.getOutput();
            if (output != null) {
                JSONObject outputJSON = processContainer(output, moduleName, true, models);
                String filename = "(" + rpc.getQName().getLocalName() + ")output";
                outputJSON.put("id", filename);
                models.put(filename, outputJSON);
            }
        }
    }

  /**
   * Processes the 'identity' statement in a yang model
   * and maps it to a 'model' in the Swagger JSON spec.
   *
   * @param module The module from which the identity stmt will be processed
   * @param models The JSONObject in which the parsed identity will be put as a 'model' obj
   * @throws JSONException
   */
  private void processIdentities(Module module, JSONObject models) throws JSONException {

    String moduleName = module.getName();
    Set<IdentitySchemaNode> idNodes =  module.getIdentities();
    _logger.debug("Processing Identities for module {} . Found {} identity statements", moduleName, idNodes.size());

    for(IdentitySchemaNode idNode : idNodes){
      JSONObject identityObj=new JSONObject();
      String identityName = idNode.getQName().getLocalName();
      _logger.debug("Processing Identity: {}", identityName);

      identityObj.put(ID_KEY, identityName);
      identityObj.put(DESCRIPTION_KEY, idNode.getDescription());

      JSONObject props = new JSONObject();
      IdentitySchemaNode baseId = idNode.getBaseIdentity();


      if(baseId==null) {
        /**
         * This is a base identity. So lets see if
         * it has sub types. If it does, then add them to the model definition.
         */
        Set<IdentitySchemaNode> derivedIds = idNode.getDerivedIdentities();

        if(derivedIds != null) {
          JSONArray subTypes = new JSONArray();
          for(IdentitySchemaNode derivedId : derivedIds){
            subTypes.put(derivedId.getQName().getLocalName());
          }
          identityObj.put(SUB_TYPES_KEY, subTypes);
        }
      } else {
        /**
         * This is a derived entity. Add it's base type & move on.
         */
        props.put(TYPE_KEY, baseId.getQName().getLocalName());
      }

      //Add the properties. For a base type, this will be an empty object as required by the Swagger spec.
      identityObj.put(PROPERTIES_KEY, props);
      models.put(identityName, identityObj);
    }
  }
    /**
     * Processes the container node and populates the moduleJSON
     *
     * @param container
     * @param moduleName
     * @param isConfig
     * @throws JSONException
     * @throws IOException
     */
    private JSONObject processContainer(ContainerSchemaNode container, String moduleName,
            boolean addSchemaStmt, JSONObject models) throws JSONException, IOException {
        return processContainer(container, moduleName, addSchemaStmt, models, (Boolean) null);
    }

    private JSONObject processContainer(ContainerSchemaNode container, String moduleName,
            boolean addSchemaStmt, JSONObject models, Boolean isConfig) throws JSONException,
            IOException {
        JSONObject moduleJSON = getSchemaTemplate();
        if (addSchemaStmt) {
            moduleJSON = getSchemaTemplate();
        } else {
            moduleJSON = new JSONObject();
        }
        moduleJSON.put(TYPE_KEY, OBJECT_TYPE);

        String containerDescription = container.getDescription();
        moduleJSON.put(DESCRIPTION_KEY, containerDescription);

        Set<DataSchemaNode> containerChildren = container.getChildNodes();
        JSONObject properties = processChildren(containerChildren, moduleName, models, isConfig);
        moduleJSON.put(PROPERTIES_KEY, properties);
        return moduleJSON;
    }

    private JSONObject processChildren(Set<DataSchemaNode> nodes, String moduleName,
            JSONObject models) throws JSONException, IOException {
        return processChildren(nodes, moduleName, models, null);
    }

    /**
     * Processes the nodes
     *
     * @param nodes
     * @param moduleName
     * @param isConfig
     * @return
     * @throws JSONException
     * @throws IOException
     */
    private JSONObject processChildren(Set<DataSchemaNode> nodes, String moduleName,
            JSONObject models, Boolean isConfig) throws JSONException, IOException {

        JSONObject properties = new JSONObject();

        for (DataSchemaNode node : nodes) {
            if (isConfig == null || node.isConfiguration() == isConfig) {

                String name = node.getQName().getLocalName();
                JSONObject property = null;
                if (node instanceof LeafSchemaNode) {
                    property = processLeafNode((LeafSchemaNode) node);
                } else if (node instanceof ListSchemaNode) {
                    property = processListSchemaNode((ListSchemaNode) node, moduleName, models, isConfig);

                } else if (node instanceof LeafListSchemaNode) {
                    property = processLeafListNode((LeafListSchemaNode) node);

                } else if (node instanceof ChoiceNode) {
                    property = processChoiceNode((ChoiceNode) node, moduleName, models);

                } else if (node instanceof AnyXmlSchemaNode) {
                    property = processAnyXMLNode((AnyXmlSchemaNode) node);

                } else if (node instanceof ContainerSchemaNode) {
                    property = processContainer((ContainerSchemaNode) node, moduleName, false,
                            models, isConfig);

                } else {
                    throw new IllegalArgumentException("Unknown DataSchemaNode type: "
                            + node.getClass());
                }

                property.putOpt(DESCRIPTION_KEY, node.getDescription());
                properties.put(name, property);
            }
        }
        return properties;
    }

    /**
     *
     * @param listNode
     * @throws JSONException
     */
    private JSONObject processLeafListNode(LeafListSchemaNode listNode) throws JSONException {
        JSONObject props = new JSONObject();
        props.put(TYPE_KEY, ARRAY_TYPE);

        JSONObject itemsVal = new JSONObject();
        processTypeDef(listNode.getType(), itemsVal);
        props.put(ITEMS_KEY, itemsVal);

        ConstraintDefinition constraints = listNode.getConstraints();
        processConstraints(constraints, props);

        return props;
    }

    /**
     *
     * @param choiceNode
     * @param moduleName
     * @throws JSONException
     * @throws IOException
     */
    private JSONObject processChoiceNode(ChoiceNode choiceNode, String moduleName, JSONObject models)
            throws JSONException, IOException {

        Set<ChoiceCaseNode> cases = choiceNode.getCases();

        JSONArray choiceProps = new JSONArray();
        for (ChoiceCaseNode choiceCase : cases) {
            String choiceName = choiceCase.getQName().getLocalName();
            JSONObject choiceProp = processChildren(choiceCase.getChildNodes(), moduleName, models);
            JSONObject choiceObj = new JSONObject();
            choiceObj.put(choiceName, choiceProp);
            choiceObj.put(TYPE_KEY, OBJECT_TYPE);
            choiceProps.put(choiceObj);
        }

        JSONObject oneOfProps = new JSONObject();
        oneOfProps.put(ONE_OF_KEY, choiceProps);
        oneOfProps.put(TYPE_KEY, OBJECT_TYPE);

        return oneOfProps;
    }

    /**
     *
     * @param constraints
     * @param props
     * @throws JSONException
     */
    private void processConstraints(ConstraintDefinition constraints, JSONObject props)
            throws JSONException {
        boolean isMandatory = constraints.isMandatory();
        props.put(REQUIRED_KEY, isMandatory);

        Integer minElements = constraints.getMinElements();
        Integer maxElements = constraints.getMaxElements();
        if (minElements != null) {
            props.put(MIN_ITEMS, minElements);
        }
        if (maxElements != null) {
            props.put(MAX_ITEMS, maxElements);
        }
    }

    /**
     * Parses a ListSchema node.
     *
     * Due to a limitation of the RAML--->JAX-RS tool, sub-properties must be in
     * a separate JSON schema file. Hence, we have to write some properties to a
     * new file, while continuing to process the rest.
     *
     * @param listNode
     * @param moduleName
     * @param isConfig
     * @return
     * @throws JSONException
     * @throws IOException
     */
    private JSONObject processListSchemaNode(ListSchemaNode listNode, String moduleName,
            JSONObject models, Boolean isConfig) throws JSONException, IOException {

        Set<DataSchemaNode> listChildren = listNode.getChildNodes();
        String fileName = (BooleanUtils.isNotFalse(isConfig)?OperationBuilder.CONFIG:OperationBuilder.OPERATIONAL) +
                                                                listNode.getQName().getLocalName();

        JSONObject childSchemaProperties = processChildren(listChildren, moduleName, models);
        JSONObject childSchema = getSchemaTemplate();
        childSchema.put(TYPE_KEY, OBJECT_TYPE);
        childSchema.put(PROPERTIES_KEY, childSchemaProperties);

        /*
         * Due to a limitation of the RAML--->JAX-RS tool, sub-properties must
         * be in a separate JSON schema file. Hence, we have to write some
         * properties to a new file, while continuing to process the rest.
         */
        // writeToFile(fileName, childSchema.toString(2), moduleName);
        childSchema.put("id", fileName);
        models.put(fileName, childSchema);

        JSONObject listNodeProperties = new JSONObject();
        listNodeProperties.put(TYPE_KEY, ARRAY_TYPE);

        JSONObject items = new JSONObject();
        items.put(REF_KEY, fileName);
        listNodeProperties.put(ITEMS_KEY, items);

        return listNodeProperties;

    }

    /**
     *
     * @param leafNode
     * @return
     * @throws JSONException
     */
    private JSONObject processLeafNode(LeafSchemaNode leafNode) throws JSONException {
        JSONObject property = new JSONObject();

        String leafDescription = leafNode.getDescription();
        property.put(DESCRIPTION_KEY, leafDescription);

        processConstraints(leafNode.getConstraints(), property);
        processTypeDef(leafNode.getType(), property);

        return property;
    }

    /**
     *
     * @param leafNode
     * @return
     * @throws JSONException
     */
    private JSONObject processAnyXMLNode(AnyXmlSchemaNode leafNode) throws JSONException {
        JSONObject property = new JSONObject();

        String leafDescription = leafNode.getDescription();
        property.put(DESCRIPTION_KEY, leafDescription);

        processConstraints(leafNode.getConstraints(), property);

        return property;
    }

    /**
     * @param property
     * @throws JSONException
     */
    private void processTypeDef(TypeDefinition<?> leafTypeDef, JSONObject property)
            throws JSONException {

        if (leafTypeDef instanceof ExtendedType) {
            processExtendedType(leafTypeDef, property);
        } else if (leafTypeDef instanceof EnumerationType) {
            processEnumType((EnumerationType) leafTypeDef, property);

        } else if (leafTypeDef instanceof BitsTypeDefinition) {
            processBitsType((BitsTypeDefinition) leafTypeDef, property);

        } else if (leafTypeDef instanceof UnionTypeDefinition) {
            processUnionType((UnionTypeDefinition) leafTypeDef, property);

        } else if (leafTypeDef instanceof IdentityrefTypeDefinition) {
      property.putOpt(TYPE_KEY, ((IdentityrefTypeDefinition) leafTypeDef).getIdentity().getQName().getLocalName());
        } else if (leafTypeDef instanceof BinaryTypeDefinition) {
            processBinaryType((BinaryTypeDefinition) leafTypeDef, property);
        } else {
            // System.out.println("In else: " + leafTypeDef.getClass());
            String jsonType = YANG_TYPE_TO_JSON_TYPE_MAPPING.get(leafTypeDef.getClass());
            if (jsonType == null) {
                jsonType = "object";
            }
            property.putOpt(TYPE_KEY, jsonType);
        }
    }

    /**
     *
     * @param leafTypeDef
     * @param property
     * @throws JSONException
     */
    private void processExtendedType(TypeDefinition<?> leafTypeDef, JSONObject property)
            throws JSONException {
        Object leafBaseType = leafTypeDef.getBaseType();
        if (leafBaseType instanceof ExtendedType) {
            // recursively process an extended type until we hit a base type
            processExtendedType((TypeDefinition<?>) leafBaseType, property);
        } else {
            List<LengthConstraint> lengthConstraints = ((ExtendedType) leafTypeDef)
                    .getLengthConstraints();
            for (LengthConstraint lengthConstraint : lengthConstraints) {
                Number min = lengthConstraint.getMin();
                Number max = lengthConstraint.getMax();
                property.putOpt(MIN_LENGTH_KEY, min);
                property.putOpt(MAX_LENGTH_KEY, max);
            }
            String jsonType = YANG_TYPE_TO_JSON_TYPE_MAPPING.get(leafBaseType.getClass());
            property.putOpt(TYPE_KEY, jsonType);
        }

    }

    /*
   *
   */
    private void processBinaryType(BinaryTypeDefinition binaryType, JSONObject property)
            throws JSONException {
        property.put(TYPE_KEY, STRING);
        JSONObject media = new JSONObject();
        media.put(BINARY_ENCODING_KEY, BASE_64);
        property.put(MEDIA_KEY, media);
    }

    /**
     *
     * @param enumLeafType
     * @param property
     * @throws JSONException
     */
    private void processEnumType(EnumerationType enumLeafType, JSONObject property)
            throws JSONException {
        List<EnumPair> enumPairs = enumLeafType.getValues();
        List<String> enumNames = new ArrayList<String>();
        for (EnumPair enumPair : enumPairs) {
            enumNames.add(enumPair.getName());
        }
        property.putOpt(ENUM, new JSONArray(enumNames));
    }

    /**
     *
     * @param bitsType
     * @param property
     * @throws JSONException
     */
    private void processBitsType(BitsTypeDefinition bitsType, JSONObject property)
            throws JSONException {
        property.put(TYPE_KEY, ARRAY_TYPE);
        property.put(MIN_ITEMS, 0);
        property.put(UNIQUE_ITEMS_KEY, true);
        JSONArray enumValues = new JSONArray();

        List<Bit> bits = bitsType.getBits();
        for (Bit bit : bits) {
            enumValues.put(bit.getName());
        }
        JSONObject itemsValue = new JSONObject();
        itemsValue.put(ENUM, enumValues);
        property.put(ITEMS_KEY, itemsValue);
    }

    /**
     *
     * @param unionType
     * @param property
     * @throws JSONException
     */
    private void processUnionType(UnionTypeDefinition unionType, JSONObject property)
            throws JSONException {

        StringBuilder type = new StringBuilder();
        for (TypeDefinition<?> typeDef : unionType.getTypes() ) {
            if( type.length() > 0 ){
                type.append( " or " );
            }
            type.append(YANG_TYPE_TO_JSON_TYPE_MAPPING.get(typeDef.getClass()));
        }

        property.put(TYPE_KEY, type );
    }

    /**
     * Helper method to generate a pre-filled JSON schema object.
     *
     * @return
     * @throws JSONException
     */
    private JSONObject getSchemaTemplate() throws JSONException {
        JSONObject schemaJSON = new JSONObject();
        schemaJSON.put(SCHEMA_KEY, SCHEMA_URL);

        return schemaJSON;
    }
}
