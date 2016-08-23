/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.xsql;

import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.xsql.XSQLProvider;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class XSQLModule {
    private static final long SLEEP_TIME_BEFORE_CREATING_TRANSACTION = 10000;

    public XSQLModule(DataBroker dataBroker, DOMDataBroker domDataBroker, SchemaService schemaService) {
        XSQLAdapter xsqlAdapter = XSQLAdapter.getInstance();
        schemaService.registerSchemaContextListener(xsqlAdapter);
        xsqlAdapter.setDataBroker(domDataBroker);
        final XSQLProvider p = new XSQLProvider();
        new Runnable() {
            @Override
            public void run() {
                try{Thread.sleep(SLEEP_TIME_BEFORE_CREATING_TRANSACTION);}catch(Exception err){}
                p.buildXSQL(dataBroker);
            }
        };
    }
}
