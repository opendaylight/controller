/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

public class Constructor {

    private final String typeName, body;
    private final boolean isPublic;

    public Constructor(String typeName, String body, boolean isPublic) {
        super();
        this.typeName = typeName;
        this.body = body;
        this.isPublic = isPublic;
    }

    // TODO add arguments if necessary

    public Constructor(String typeName, String body) {
        this(typeName, body, true);
    }

    public String getTypeName() {
        return typeName;
    }

    public String getBody() {
        return body;
    }

    public boolean isPublic() {
        return isPublic;
    }

}
