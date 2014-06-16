/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.swagger;

import java.util.List;

/**
 * Implementation of swagger spec (see <a href=
 * "https://github.com/wordnik/swagger-spec/blob/master/versions/1.2.md#51-resource-listing"
 * > https://github.com/wordnik/swagger-spec/blob/master/versions/1.2.md#51-
 * resource-listing</a>)
 */
public class ResourceList {
    private String apiVersion;
    private String swaggerVersion;
    private List<Resource> apis;

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

    public List<Resource> getApis() {
        return apis;
    }

    public void setApis(List<Resource> apis) {
        this.apis = apis;
    }
}
