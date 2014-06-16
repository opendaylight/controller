/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.swagger;

import java.util.List;

import org.json.JSONObject;

/**
 * Implementation of swagger spec (see <a href=
 * "https://github.com/wordnik/swagger-spec/blob/master/versions/1.2.md#52-api-declaration"
 * > https://github.com/wordnik/swagger-spec/blob/master/versions/1.2.md#52-api-
 * declaration</a>)
 */
public class ApiDeclaration {
    private String apiVersion;
    private String swaggerVersion;
    private String basePath;
    private String resourcePath;
    private List<String> produces;
    private List<Api> apis;
    private JSONObject models;

    public JSONObject getModels() {
        return models;
    }

    public void setModels(JSONObject models) {
        this.models = models;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getSwaggerVersion() {
        return swaggerVersion;
    }

    public void setSwaggerVersion(String swaggerVersion) {
        this.swaggerVersion = swaggerVersion;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public List<String> getProduces() {
        return produces;
    }

    public void setProduces(List<String> produces) {
        this.produces = produces;
    }

    public List<Api> getApis() {
        return apis;
    }

    public void setApis(List<Api> apis) {
        this.apis = apis;
    }

    public boolean hasApi() {
        return (apis != null && !apis.isEmpty());
    }

    public boolean hasModel() {
        return (models != null && models.length() > 0);
    }
}
