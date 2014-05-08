/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.api;

import org.opendaylight.yangtools.yang.common.QName;

public class Draft02 {
    public static interface MediaTypes {
        String API = "application/yang.api";
        String DATASTORE = "application/yang.datastore";
        String DATA = "application/yang.data";
        String OPERATION = "application/yang.operation";
        String PATCH = "application/yang.patch";
        String PATCH_STATUS = "application/yang.patch-status";
        String STREAM = "application/yang.stream";
    }

    public static interface RestConfModule {
        String REVISION = "2013-10-19";

        String NAME = "ietf-restconf";

        String NAMESPACE = "urn:ietf:params:xml:ns:yang:ietf-restconf";

        String RESTCONF_GROUPING_SCHEMA_NODE = "restconf";

        String RESTCONF_CONTAINER_SCHEMA_NODE = "restconf";

        String MODULES_CONTAINER_SCHEMA_NODE = "modules";

        String MODULE_LIST_SCHEMA_NODE = "module";

        String STREAMS_CONTAINER_SCHEMA_NODE = "streams";

        String STREAM_LIST_SCHEMA_NODE = "stream";

        String OPERATIONS_CONTAINER_SCHEMA_NODE = "operations";

        String ERRORS_GROUPING_SCHEMA_NODE = "errors";

        String ERRORS_CONTAINER_SCHEMA_NODE = "errors";

        String ERROR_LIST_SCHEMA_NODE = "error";

        QName IETF_RESTCONF_QNAME = QName.create(Draft02.RestConfModule.NAMESPACE, Draft02.RestConfModule.REVISION,
                Draft02.RestConfModule.NAME);

        QName ERRORS_CONTAINER_QNAME = QName.create(IETF_RESTCONF_QNAME, ERRORS_CONTAINER_SCHEMA_NODE);

        QName ERROR_LIST_QNAME = QName.create(IETF_RESTCONF_QNAME, ERROR_LIST_SCHEMA_NODE);

        QName ERROR_TYPE_QNAME = QName.create(IETF_RESTCONF_QNAME, "error-type");

        QName ERROR_TAG_QNAME = QName.create(IETF_RESTCONF_QNAME, "error-tag");

        QName ERROR_APP_TAG_QNAME = QName.create(IETF_RESTCONF_QNAME, "error-app-tag");

        QName ERROR_MESSAGE_QNAME = QName.create(IETF_RESTCONF_QNAME, "error-message");

        QName ERROR_INFO_QNAME = QName.create(IETF_RESTCONF_QNAME, "error-info");
    }

    public static interface Paths {

    }
}
