/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc.dto;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.io.Serializable;
import java.net.URI;

public class RouteIdentifierImpl implements RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier>,Serializable {

  transient ObjectMapper mapper = new ObjectMapper();

  private QName context;
  private QName type;
  private InstanceIdentifier route;

  @Override
  public QName getContext() {
    return this.context;
  }

  @Override
  public QName getType() {
    return this.type;
  }

  @Override
  public InstanceIdentifier getRoute() {
    return this.route;
  }

  public void setContext(QName context) {
    this.context = context;
  }

  public void setType(QName type) {
    this.type = type;
  }

  public void setRoute(InstanceIdentifier route) {
    this.route = route;
  }

  @Override
  public String toString() {
    try {
      return mapper.writeValueAsString(this);
    } catch (Throwable e) {
      //do nothing
    }

    return super.toString();
  }

  public RpcRouter.RouteIdentifier fromString(String input)
      throws Exception {

    JsonNode root = mapper.readTree(input);
    this.context  = parseQName(root.get("context"));
    this.type     = parseQName(root.get("type"));

    return this;
  }

  private QName parseQName(JsonNode node){
    if (node == null) return null;

    String namespace = (node.get("namespace") != null) ?
                       node.get("namespace").asText()  : "";

    String localName = (node.get("localName") != null) ?
                       node.get("localName").asText() : "";

    URI uri = URI.create(namespace);
    return new QName(uri, localName);
  }
}
