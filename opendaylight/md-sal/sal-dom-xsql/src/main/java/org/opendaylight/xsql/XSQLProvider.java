package org.opendaylight.xsql;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.XSQL;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.XSQLBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by root on 6/26/14.
 */
public class XSQLProvider implements AutoCloseable {

    public static final InstanceIdentifier<XSQL> ID = InstanceIdentifier.builder(XSQL.class).build();
    private static final Logger LOG = LoggerFactory.getLogger(XSQLProvider.class);

    public void close() {
    }

    public XSQL buildXSQL(DataProviderService dps) {
            XSQLBuilder builder = new XSQLBuilder();
            builder.setPort("34343");
            XSQL xsql = builder.build();
            if (dps != null) {
                final DataModificationTransaction t = dps.beginTransaction();
                t.removeOperationalData(ID);
                t.putOperationalData(ID,xsql);

                try {
                    t.commit().get();
                } catch (InterruptedException | ExecutionException e) {
                   LOG.warn("Failed to update toaster status, operational otherwise", e);
                }
            }
        return xsql;
    }
}
