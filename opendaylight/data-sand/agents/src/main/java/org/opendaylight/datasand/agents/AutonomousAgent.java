package org.opendaylight.datasand.agents;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
import org.opendaylight.datasand.network.NetworkID;
import org.opendaylight.datasand.network.Packet;
import org.opendaylight.datasand.network.PriorityLinkedList;

public abstract class AutonomousAgent implements Runnable {

    private NetworkID agentID = null;
    private AutonomousAgentManager manager = null;
    protected PriorityLinkedList<Packet> incoming = new PriorityLinkedList<Packet>();
    protected boolean working = false;
    private Packet currentFrame = null;
    private List<RepetitiveFrameEntry> repetitiveTasks = new ArrayList<RepetitiveFrameEntry>();
    private long lastRepetitiveCheck = 0;
    public boolean _ForTestOnly_pseudoSendEnabled = false;

    public AutonomousAgent(int subSystemID,AutonomousAgentManager _manager) {
        this.agentID = new NetworkID(_manager.getNetworkNode().getLocalHost().getIPv4Address(),_manager.getNetworkNode().getLocalHost().getPort(), subSystemID);
        this.manager = _manager;
        this.manager.registerAgent(this);
    }

    public NetworkID getAgentID() {
        return this.agentID;
    }

    public void addFrame(Packet p) {
        incoming.add(p, p.getPriority());
    }

    public void checkForRepetitive() {
        if (System.currentTimeMillis() - lastRepetitiveCheck > 10000) {
            for (RepetitiveFrameEntry e : repetitiveTasks) {
                if (e.shouldExecute()) {
                    incoming.add(e.frame, e.priority);
                }
            }
            lastRepetitiveCheck = System.currentTimeMillis();
        }
    }

    public void pop() {
        working = true;
        currentFrame = incoming.next();
    }

    public void run() {
        currentFrame.decode(manager.getTypeDescriptorsContainer());
        if (currentFrame.getDecodedObject() instanceof ISideTask) {
            this.getAgentManager().runSideTask((ISideTask) currentFrame.getDecodedObject());
        } else {
            processNext(currentFrame, currentFrame.getDecodedObject());
        }
        currentFrame = null;
        synchronized (manager.getSyncObject()) {
            working = false;
            manager.getSyncObject().notifyAll();
        }
    }

    public abstract void processNext(Packet frame, Object obj);

    public abstract void start();

    public abstract String getName();

    public void send(Object obj, NetworkID destination) {
        if(_ForTestOnly_pseudoSendEnabled) return;
        ByteArrayEncodeDataContainer ba = new ByteArrayEncodeDataContainer(1024,this.manager.getTypeDescriptorsContainer());
        ba.getEncoder().encodeObject(obj, ba,this.manager.getTypeDescriptorsContainer().getElementClass(obj));
        manager.getNetworkNode().send(ba.getData(), this.agentID, destination);
    }

    public void send(byte data[], NetworkID destination) {
        manager.getNetworkNode().send(data, this.agentID, destination);
    }

    public void addRepetitiveFrame(Packet frame, long interval,long intervalStart, int priority) {
        RepetitiveFrameEntry entry = new RepetitiveFrameEntry(frame, interval,intervalStart, priority);
        if(entry.shouldExecute()){
            incoming.add(entry.frame, entry.priority);
            this.getAgentManager().messageWasEnqueued();
        }
        repetitiveTasks.add(entry);
    }

    private static class RepetitiveFrameEntry {
        private Packet frame = null;
        private long interval = -1;
        private long intervalStart = -1;
        private long lastExecuted = System.currentTimeMillis();
        private boolean started = false;
        private int priority = 2;

        public RepetitiveFrameEntry(Packet _frame, long _interval,long _intervalStart, int _priority) {
            this.frame = _frame;
            this.interval = _interval;
            this.intervalStart = _intervalStart;
            this.priority = _priority;
        }

        public boolean shouldExecute() {
            if (System.currentTimeMillis() - lastExecuted > interval){
                lastExecuted = System.currentTimeMillis();
                return true;
            }

            if (!started && (System.currentTimeMillis() - lastExecuted > intervalStart || intervalStart == 0)) {
                started = true;
                lastExecuted = System.currentTimeMillis();
                return true;
            }
            return false;
        }
    }

    protected AutonomousAgentManager getAgentManager() {
        return this.manager;
    }
}
