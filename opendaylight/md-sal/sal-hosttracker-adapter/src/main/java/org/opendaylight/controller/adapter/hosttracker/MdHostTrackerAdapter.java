package org.opendaylight.controller.adapter.hosttracker;

import java.util.concurrent.Future;

import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.hosttracker.rev131013.AddStaticHostInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.hosttracker.rev131013.FindHostInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.hosttracker.rev131013.FindHostOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.hosttracker.rev131013.GetActiveStaticHostsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.hosttracker.rev131013.GetAllHostsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.hosttracker.rev131013.GetHostNetworkHiearchyInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.hosttracker.rev131013.GetInactiveStaticHostsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.hosttracker.rev131013.OpendaylightHosttrackerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.hosttracker.rev131013.QueryHostInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.hosttracker.rev131013.QueryHostOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.hosttracker.rev131013.RemoveStaticHostInput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdHostTrackerAdapter implements OpendaylightHosttrackerService, IfNewHostNotify {
	private static final Logger log = LoggerFactory.getLogger(MdHostTrackerAdapter.class);
	private NotificationProviderService notificationProvider;
	private IfIptoHost hostTracker;
	
	@Override
	public Future<RpcResult<Void>> addStaticHost(AddStaticHostInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<FindHostOutput>> findHost(FindHostInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<GetActiveStaticHostsOutput>> getActiveStaticHosts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<GetAllHostsOutput>> getAllHosts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<Void>> getHostNetworkHiearchy(
			GetHostNetworkHiearchyInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<GetInactiveStaticHostsOutput>> getInactiveStaticHosts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<QueryHostOutput>> queryHost(QueryHostInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<Void>> removeStaticHost(RemoveStaticHostInput input) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void setHostTracker(IfIptoHost hostTracker) {
        log.debug("Setting HostTracker");
        this.hostTracker = hostTracker;
    }

    public void unsetHostTracker(IfIptoHost s) {
        log.debug("UNSetting HostTracker");
        if (this.hostTracker == s) {
            this.hostTracker = null;
        }
    }

	@Override
	public void notifyHTClient(HostNodeConnector host) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyHTClientHostRemoved(HostNodeConnector host) {
		// TODO Auto-generated method stub
		
	}

	public NotificationProviderService getNotificationProvider() {
		return notificationProvider;
	}

	public void setNotificationProvider(NotificationProviderService notificationProvider) {
		this.notificationProvider = notificationProvider;
	}

}
