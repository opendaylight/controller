package org.opendaylight.xsql;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.XSQL;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.XSQLBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Created by root on 6/26/14.
 */
public class XSQLProvider implements AutoCloseable {

    public static final InstanceIdentifier<XSQL> ID = InstanceIdentifier.builder(XSQL.class).build();
    //public static final InstanceIdentifier<SalTest> ID2 = InstanceIdentifier.builder(SalTest.class).build();

    public void close() {
    }

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
                    /*
                    WriteTransaction tx = dps.newReadWriteTransaction();
                    tx.delete(LogicalDatastoreType.OPERATIONAL, ID2);
                    tx.put(LogicalDatastoreType.OPERATIONAL,ID2,XSQLModule.buildTestElement(null, 101, true, true, true, true, true, 2));
                    XSQLAdapter.log("Submitting Sal Test...");
                    tx.submit();
                    */
                }
            } catch (Exception e) {
                XSQLAdapter.log(e);
            }
        return xsql;
    }
}
