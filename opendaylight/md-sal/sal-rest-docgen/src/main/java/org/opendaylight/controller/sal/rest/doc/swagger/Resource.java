/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.swagger;

/**
 * Implementation of swagger spec (see <a href=
 * "https://github.com/wordnik/swagger-spec/blob/master/versions/1.2.md#512-resource-object"
 * > https://github.com/wordnik/swagger-spec/blob/master/versions/1.2.md#512-
 * resource-object</a>)
 */
public class Resource {
    private String path;
    private String description;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
