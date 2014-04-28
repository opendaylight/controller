/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.model.builder;

import org.opendaylight.controller.sal.rest.doc.swagger.Operation;
import org.opendaylight.controller.sal.rest.doc.swagger.Parameter;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public final class OperationBuilder {

  /**
   *
   */
  public static class Get{

    protected Operation spec;
    protected DataSchemaNode schemaNode;
    private final String METHOD_NAME = "GET";

    public Get(DataSchemaNode node){
      this.schemaNode = node;
      spec = new Operation();
      spec.setMethod(METHOD_NAME);
      spec.setNickname(METHOD_NAME + "-" + node.getQName().getLocalName());
      spec.setType(node.getQName().getLocalName());
      spec.setNotes(node.getDescription());
    }

    public Get pathParams(List<Parameter> params){
      List<Parameter> pathParameters = new ArrayList<>(params);
      spec.setParameters(pathParameters);
      return this;
    }

    public Operation build(){
      return spec;
    }
  }

  /**
   *
   */
  public static class Put{
    protected Operation spec;
    protected DataSchemaNode schemaNode;
    private final String METHOD_NAME = "PUT";

    public Put(DataSchemaNode node){
      this.schemaNode = node;
      spec = new Operation();
      spec.setType(node.getQName().getLocalName());
      spec.setNotes(node.getDescription());
    }

    public Put pathParams(List<Parameter> params){
      List<Parameter> parameters = new ArrayList<>(params);
      Parameter payload = new Parameter();
      payload.setParamType("body");
      payload.setType(schemaNode.getQName().getLocalName());
      parameters.add(payload);
      spec.setParameters(parameters);
      return this;
    }

    public Operation build(){
      spec.setMethod(METHOD_NAME);
      spec.setNickname(METHOD_NAME + "-" + schemaNode.getQName().getLocalName());
      return spec;
    }
  }

  /**
   *
   */
  public static final class Post extends Put{

    private final String METHOD_NAME = "POST";

    public Post(DataSchemaNode node){
      super(node);
    }

    public Operation build(){
      spec.setMethod(METHOD_NAME);
      spec.setNickname(METHOD_NAME + "-" + schemaNode.getQName().getLocalName());
      return spec;
    }
  }

  /**
   *
   */
  public static final class Delete extends Get {
    private final String METHOD_NAME = "DELETE";

    public Delete(DataSchemaNode node){
      super(node);
    }

    public Operation build(){
      spec.setMethod(METHOD_NAME);
      spec.setNickname(METHOD_NAME + "-" + schemaNode.getQName().getLocalName());
      spec.setType(null);
      return spec;
    }
  }
}
