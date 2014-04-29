/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.google.common.base.Preconditions;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.rest.doc.model.builder.OperationBuilder;
import org.opendaylight.controller.sal.rest.doc.swagger.*;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class gathers all yang defined {@link Module}s and generates Swagger compliant documentation.
 */
public class ApiDocGenerator {

  private static Logger _logger = LoggerFactory.getLogger(ApiDocGenerator.class);

  private static final ApiDocGenerator INSTANCE = new ApiDocGenerator();
  private ObjectMapper mapper = new ObjectMapper();
  private final ModelGenerator jsonConverter = new ModelGenerator();

  private SchemaService schemaService;

  private final String API_VERSION = "1.0.0";
  private final String SWAGGER_VERSION = "1.2";
  private final String RESTCONF_CONTEXT_ROOT = "restconf";
  private final DateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  //For now its {@link HashMap}. It will be changed to thread-safe Map when schema change listener is implemented.
  private final Map<String, ApiDeclaration> MODULE_DOC_CACHE = new HashMap<String, ApiDeclaration>();

  private ApiDocGenerator(){
    mapper.registerModule(new JsonOrgModule());
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
  }

  /**
   * Returns singleton instance
   * @return
   */
  public static ApiDocGenerator getInstance() {
    return INSTANCE;
  }

  /**
   *
   * @param schemaService
   */
  public void setSchemaService(SchemaService schemaService) {
    this.schemaService = schemaService;
  }
  /**
   *
   * @param uriInfo
   * @return  list of modules converted to swagger compliant resource list.
   */
  public ResourceList getResourceListing(UriInfo uriInfo) {

    Preconditions.checkState(schemaService != null);
    SchemaContext schemaContext = schemaService.getGlobalContext();
    Preconditions.checkState(schemaContext != null);

    Set<Module> modules = schemaContext.getModules();

    ResourceList resourceList = new ResourceList();
    resourceList.setApiVersion(API_VERSION);
    resourceList.setSwaggerVersion(SWAGGER_VERSION);

    List<Resource> resources = new ArrayList<>(modules.size());
    _logger.info("Modules found [{}]", modules.size());

    for (Module module : modules) {
      Resource resource = new Resource();
      String revisionString = SIMPLE_DATE_FORMAT.format(module.getRevision());

      _logger.debug("Working on [{},{}]...", module.getName(), revisionString);
      ApiDeclaration doc = getApiDeclaration(module.getName(), revisionString, uriInfo);

      if (doc != null) {
        URI uri = uriInfo.getRequestUriBuilder().
            path(generateCacheKey(module.getName(), revisionString)).
            build();

        resource.setPath(uri.toASCIIString());
        resources.add(resource);
      } else {
        _logger.debug("Could not generate doc for {},{}", module.getName(), revisionString);
      }
    }

    resourceList.setApis(resources);

    return resourceList;
  }

  public ApiDeclaration getApiDeclaration(String module, String revision, UriInfo uriInfo) {

    //Lookup cache
    String cacheKey = generateCacheKey(module, revision);

    if (MODULE_DOC_CACHE.containsKey(cacheKey)) {
      _logger.debug("Serving from cache for {}", cacheKey);
      return MODULE_DOC_CACHE.get(cacheKey);
    }

    Date rev = null;
    try {
      rev = SIMPLE_DATE_FORMAT.parse(revision);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e);
    }

    SchemaContext schemaContext = schemaService.getGlobalContext();
    Preconditions.checkState(schemaContext != null);

    Module m = schemaContext.findModuleByName(module, rev);
    Preconditions.checkArgument(m != null, "Could not find module by name,revision: " + module + "," + revision);

    String basePath = new StringBuilder(uriInfo.getBaseUri().getScheme())
        .append("://")
        .append(uriInfo.getBaseUri().getHost())
        .append(":")
        .append(uriInfo.getBaseUri().getPort())
        .append("/")
        .append(RESTCONF_CONTEXT_ROOT)
        .toString();

    ApiDeclaration doc = getSwaggerDocSpec(m, basePath);
    MODULE_DOC_CACHE.put(cacheKey, doc);
    return doc;
  }

  public ApiDeclaration getSwaggerDocSpec(Module m, String basePath) {
    ApiDeclaration doc = new ApiDeclaration();
    doc.setApiVersion(API_VERSION);
    doc.setSwaggerVersion(SWAGGER_VERSION);
    doc.setBasePath(basePath);
    doc.setProduces(Arrays.asList("application/json", "application/xml"));

    List<Api> apis = new ArrayList<Api>();

    Set<DataSchemaNode> dataSchemaNodes = m.getChildNodes();
    _logger.debug("child nodes size [{}]", dataSchemaNodes.size());
    for (DataSchemaNode node : dataSchemaNodes) {
      if ((node instanceof ListSchemaNode) || (node instanceof ContainerSchemaNode)) {

        _logger.debug("Is Configuration node [{}] [{}]", node.isConfiguration(), node.getQName().getLocalName());

        List<Parameter> pathParams = null;
        if (node.isConfiguration()) {
          pathParams = new ArrayList<Parameter>();
          String resourcePath = "/config/" + m.getName() + ":";
          addApis(node, apis, resourcePath, pathParams);

        }

        pathParams = new ArrayList<Parameter>();
        String resourcePath = "/operational/" + m.getName() + ":";
        addApis(node, apis, resourcePath, pathParams);
      }
    }

    Set<RpcDefinition> rpcs = m.getRpcs();
    for (RpcDefinition rpcDefinition : rpcs) {
      String resourcePath = "/operations/" + m.getName() + ":";
      addRpcs(rpcDefinition, apis, resourcePath);

    }
    _logger.debug("Number of APIs found [{}]", apis.size());
    doc.setApis(apis);
    JSONObject models = null;

    try {
      models = jsonConverter.convertToJsonSchema(m);
      doc.setModels(models);
      _logger.debug(mapper.writeValueAsString(doc));
    } catch (IOException | JSONException e) {
      e.printStackTrace();
    }

    return doc;
  }

  private String generateCacheKey(Module m) {
    return generateCacheKey(m.getName(), SIMPLE_DATE_FORMAT.format(m.getRevision()));
  }

  private String generateCacheKey(String module, String revision) {
    return module + "," + revision;
  }

  private void addApis(DataSchemaNode node,
                       List<Api> apis,
                       String parentPath,
                       List<Parameter> parentPathParams) {

    Api api = new Api();
    List<Parameter> pathParams = new ArrayList<Parameter>(parentPathParams);

    String resourcePath = parentPath + createPath(node, pathParams) + "/";
    _logger.debug("Adding path: [{}]", resourcePath);
    api.setPath(resourcePath);
    api.setOperations(operations(node, pathParams));
    apis.add(api);
    if ((node instanceof ListSchemaNode) || (node instanceof ContainerSchemaNode)) {
      DataNodeContainer schemaNode = (DataNodeContainer) node;
      Set<DataSchemaNode> dataSchemaNodes = schemaNode.getChildNodes();

      for (DataSchemaNode childNode : dataSchemaNodes) {
        addApis(childNode, apis, resourcePath, pathParams);
      }
    }

  }

  private void addRpcs(RpcDefinition rpcDefn, List<Api> apis, String parentPath) {
    Api rpc = new Api();
    String resourcePath = parentPath + rpcDefn.getQName().getLocalName();
    rpc.setPath(resourcePath);

    Operation operationSpec = new Operation();
    operationSpec.setMethod("POST");
    operationSpec.setNotes(rpcDefn.getDescription());
    operationSpec.setNickname(rpcDefn.getQName().getLocalName());
    rpc.setOperations(Arrays.asList(operationSpec));

    apis.add(rpc);
  }

  /**
   * @param node
   * @param pathParams
   * @return
   */
  private List<Operation> operations(DataSchemaNode node, List<Parameter> pathParams) {
    List<Operation> operations = new ArrayList<>();

    OperationBuilder.Get getBuilder = new OperationBuilder.Get(node);
    operations.add(getBuilder.pathParams(pathParams).build());

    OperationBuilder.Delete deleteBuilder = new OperationBuilder.Delete(node);
    operations.add(deleteBuilder.pathParams(pathParams).build());

    if (node.isConfiguration()) {
      OperationBuilder.Post postBuilder = new OperationBuilder.Post(node);
      operations.add(postBuilder.pathParams(pathParams).build());

      OperationBuilder.Put putBuilder = new OperationBuilder.Put(node);
      operations.add(putBuilder.pathParams(pathParams).build());
    }
    return operations;
  }

  private String createPath(final DataSchemaNode schemaNode, List<Parameter> pathParams) {
    ArrayList<LeafSchemaNode> pathListParams = new ArrayList<LeafSchemaNode>();
    StringBuilder path = new StringBuilder();
    QName _qName = schemaNode.getQName();
    String localName = _qName.getLocalName();
    path.append(localName);

    if ((schemaNode instanceof ListSchemaNode)) {
      final List<QName> listKeys = ((ListSchemaNode) schemaNode).getKeyDefinition();
      for (final QName listKey : listKeys) {
        {
          DataSchemaNode _dataChildByName = ((DataNodeContainer) schemaNode).getDataChildByName(listKey);
          pathListParams.add(((LeafSchemaNode) _dataChildByName));

          String pathParamIdentifier = new StringBuilder("/{").append(listKey.getLocalName()).append("}").toString();
          path.append(pathParamIdentifier);

          Parameter pathParam = new Parameter();
          pathParam.setName(listKey.getLocalName());
          pathParam.setDescription(_dataChildByName.getDescription());
          pathParam.setType("string");
          pathParam.setParamType("path");

          pathParams.add(pathParam);
        }
      }
    }
    return path.toString();
  }

}
