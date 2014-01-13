package org.opendaylight.controller.sal.connect.netconf;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class InventoryUtils {

    private static final URI INVENTORY_NAMESPACE = URI.create("urn:opendaylight:inventory");
    private static final Date INVENTORY_REVISION = dateFromString("2013-08-19");
    public static final QName INVENTORY_NODES = new QName(INVENTORY_NAMESPACE, INVENTORY_REVISION, "nodes");
    public static final QName INVENTORY_NODE = new QName(INVENTORY_NAMESPACE, INVENTORY_REVISION, "node");
    public static final QName INVENTORY_ID = new QName(INVENTORY_NAMESPACE, INVENTORY_REVISION, "id");
    public static final QName INVENTORY_CONNECTED = new QName(INVENTORY_NAMESPACE, INVENTORY_REVISION, "connected");
    public static final QName NETCONF_INVENTORY_INITIAL_CAPABILITY = new QName(
            URI.create("urn:opendaylight:netconf-node-inventory"), dateFromString("2014-01-08"), "initial-capability");

    public static final InstanceIdentifier INVENTORY_PATH = InstanceIdentifier.builder().node(INVENTORY_NODES)
            .toInstance();
    public static final QName NETCONF_INVENTORY_MOUNT = null;

    /**
     * Converts date in string format yyyy-MM-dd to java.util.Date.
     * 
     * @return java.util.Date conformant to string formatted date yyyy-MM-dd.
     */
    private static Date dateFromString(final String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
