package org.opendaylight.controller.sal.connect.netconf

import org.opendaylight.controller.sal.core.api.Broker.ProviderSession
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService
import org.opendaylight.controller.md.sal.common.api.data.DataProvisionService
import org.opendaylight.controller.sal.core.api.data.DataProviderService
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.common.QName
import static org.opendaylight.controller.sal.connect.netconf.InventoryUtils.*;
import static extension org.opendaylight.controller.sal.connect.netconf.NetconfInventoryUtils.*;

import org.opendaylight.controller.sal.core.api.data.DataChangeListener
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher
import java.io.OptionalDataException
import com.google.common.base.Optional
import java.net.SocketAddress
import java.net.InetSocketAddress

class NetconfDeviceManager {

    val Map<InstanceIdentifier, NetconfDevice> devices = new ConcurrentHashMap;

    var ProviderSession session;

    @Property
    var DataProviderService dataService;
    
    @Property
    var MountProvisionService mountService;
    
    val nodeUpdateListener = new NetconfInventoryListener(this);


    @Property
    var NetconfClientDispatcher dispatcher;

    def void start() {
        dataService?.registerDataChangeListener(INVENTORY_PATH, nodeUpdateListener);
        if(dispatcher == null) {
        dispatcher = new NetconfClientDispatcher(Optional.absent);
        }
    }

    def netconfNodeAdded(InstanceIdentifier path, CompositeNode node) {
        val address = node.endpointAddress;
        val port = Integer.parseInt(node.endpointPort);
        netconfNodeAdded(path,new InetSocketAddress(address,port))

    }
    
    def netconfNodeAdded(InstanceIdentifier path, InetSocketAddress address) {
    
        val mountPointPath = path;
        val mountPoint = mountService.createOrGetMountPoint(mountPointPath);
        val localPath = InstanceIdentifier.builder().toInstance;
        val netconfDevice = new NetconfDevice(mountPoint,localPath);
        netconfDevice.setSocketAddress(address);
        netconfDevice.start(dispatcher);
    }

    def netconfNodeRemoved(InstanceIdentifier path) {
    
    }

}

class NetconfInventoryListener implements DataChangeListener {

    val NetconfDeviceManager manager;

    new(NetconfDeviceManager manager) {
        this.manager = manager;
    }

    override onDataChanged(DataChangeEvent<InstanceIdentifier, CompositeNode> change) {
        
        //manager.netconfNodeAdded(path, change);
    }
}
