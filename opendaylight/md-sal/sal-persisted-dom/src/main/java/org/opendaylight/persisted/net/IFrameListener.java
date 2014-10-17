package org.opendaylight.persisted.net;

public interface IFrameListener {
	public void process(Packet frame);
	public void processDestinationUnreachable(Packet frame);
	public void processBroadcast(Packet frame);
	public void processMulticast(Packet frame);
}
