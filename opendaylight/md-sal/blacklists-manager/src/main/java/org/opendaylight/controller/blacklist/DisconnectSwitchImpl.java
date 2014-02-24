package org.opendaylight.controller.blacklist;



import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightDisconnectSwitchInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryService;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class DisconnectSwitchImpl implements IBlackList {
    private static final Logger LOG = LoggerFactory.getLogger(DisconnectSwitchImpl.class);

    private static DataBrokerService dataBrokerService;
    private static ProviderContext pc;
    public static OpendaylightInventoryService oInventoryService;
    private final BundleContext ctx;

    public DisconnectSwitchImpl(BundleContext ctx)
    {
        this.ctx = ctx;
    	  
    }
    public void onSessionInitiated(ProviderContext session) {
        pc = session;
        dataBrokerService = session.getSALService(DataBrokerService.class);
        oInventoryService = session.getRpcService(OpendaylightInventoryService.class);
    }
    
    private static NodeRef createNodeRef(String string) {
        NodeKey key = new NodeKey(new NodeId(string));
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> path = InstanceIdentifier.builder().node(Nodes.class).node(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, key)
                .toInstance();

        return new NodeRef(path);
    }
	@Override
	public void  disconnectSwitch(String s)  {
		// TODO Auto-generated method stub
		DataModification<InstanceIdentifier<?>, DataObject> modification = dataBrokerService.beginTransaction();
		OpendaylightDisconnectSwitchInputBuilder opendaylightDisconnectSwitch = new OpendaylightDisconnectSwitchInputBuilder();
		String[] data = s.split(":");
		if(data.length == 2)
		{
			if(data[0].equalsIgnoreCase("openflow"))
			{
				try
				{
		        opendaylightDisconnectSwitch.setNode(createNodeRef(s));
		        oInventoryService.opendaylightDisconnectSwitch(opendaylightDisconnectSwitch.build());
				}catch(IllegalStateException e)
				{
					System.out.println("Invalid Datapath ID..");
				}
			}
			else
			{
				System.out.println("Invalid DPN");
				return;
			}
		}
	}
}
