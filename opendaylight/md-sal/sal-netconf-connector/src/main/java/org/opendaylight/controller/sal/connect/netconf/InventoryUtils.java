package org.opendaylight.controller.sal.connect.netconf;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class InventoryUtils {

    private static final URI INVENTORY_NAMESPACE = URI.create("urn:opendaylight:inventory");
    private static final Date INVENTORY_REVISION = date();
    public static final QName INVENTORY_NODES = new QName(INVENTORY_NAMESPACE, INVENTORY_REVISION, "nodes");
    public static final QName INVENTORY_NODE = new QName(INVENTORY_NAMESPACE, INVENTORY_REVISION, "node");
    public static final QName INVENTORY_ID = new QName(INVENTORY_NAMESPACE, INVENTORY_REVISION, "id");

    public static final InstanceIdentifier INVENTORY_PATH = InstanceIdentifier.builder().node(INVENTORY_NODES)
            .toInstance();
    public static final QName NETCONF_INVENTORY_MOUNT = null;
    
    
    
    private static Date date() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return formatter.parse("2013-08-19");
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    
    
}
