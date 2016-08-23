/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.xsql;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.XSQL;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.XSQLBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class XSQLProvider {

    public static final InstanceIdentifier<XSQL> ID = InstanceIdentifier.builder(XSQL.class).build();

    public XSQL buildXSQL(DataBroker dps) {
            XSQLAdapter.log("Building XSL...");
            XSQLBuilder builder = new XSQLBuilder();
            builder.setPort("34343");
            XSQL xsql = builder.build();
            try {
                if (dps != null) {
                    XSQLAdapter.log("Starting TRansaction...");
                    WriteTransaction t = dps.newReadWriteTransaction();
                    t.delete(LogicalDatastoreType.OPERATIONAL, ID);
                    t.put(LogicalDatastoreType.OPERATIONAL,ID,xsql);
                    XSQLAdapter.log("Submitting...");
                    t.submit();
                }
            } catch (Exception e) {
                XSQLAdapter.log(e);
            }
        return xsql;
    }
}
