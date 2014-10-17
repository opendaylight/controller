package org.opendaylight.persisted.autoagents;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.persisted.codec.EncodeDataContainer;
import org.opendaylight.persisted.codec.EncodeUtils;
import org.opendaylight.persisted.codec.ISerializer;
import org.opendaylight.persisted.net.NetworkID;
import org.opendaylight.persisted.net.Packet;
import org.opendaylight.persisted.net.PriorityLinkedList;

public abstract class AutonomousAgent implements Runnable {

    private NetworkID handlerID = null;
    private AutonomousAgentManager manager = null;
    protected PriorityLinkedList<Packet> incoming = new PriorityLinkedList<Packet>();
    protected boolean working = false;
    private Packet currentFrame = null;
    private List<RepetitiveFrameEntry> repetitiveTasks = new ArrayList<RepetitiveFrameEntry>();
    private long lastRepetitiveCheck = 0;

    public AutonomousAgent(int subSystemID, NetworkID localHost,
            AutonomousAgentManager _manager) {
        this.handlerID = new NetworkID(localHost.getIPv4Address(),
                localHost.getPort(), subSystemID);
        this.manager = _manager;
    }

    public NetworkID getHandlerID() {
        return this.handlerID;
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
        currentFrame.decode();
        if (currentFrame.getDecodedObject() instanceof ISideTask) {
            this.getHandlerManager().runSideTask(
                    (ISideTask) currentFrame.getDecodedObject());
        } else {
            processNext(currentFrame, currentFrame.getDecodedObject());
        }

        currentFrame = null;
        working = false;
        synchronized (manager.getSyncObject()) {
            manager.getSyncObject().notifyAll();
        }
    }

    public abstract void processNext(Packet frame, Object obj);

    public abstract void start();

    public abstract String getName();

    public void send(Object obj, NetworkID destination) {
        ISerializer ser = EncodeUtils.getSerializer(obj.getClass());
        EncodeDataContainer ba = new EncodeDataContainer(1024);
        ser.encode(obj, ba);
        manager.getNetworkNode()
                .send(ba.getData(), this.handlerID, destination);
    }

    public void send(byte data[], NetworkID destination) {
        manager.getNetworkNode().send(data, this.handlerID, destination);
    }

    public void addRepetitiveFrame(Packet frame, long interval,
            long intervalStart, int priority) {
        RepetitiveFrameEntry entry = new RepetitiveFrameEntry(frame, interval,
                intervalStart, priority);
        repetitiveTasks.add(entry);
    }

    private static class RepetitiveFrameEntry {
        private Packet frame = null;
        private long interval = -1;
        private long intervalStart = -1;
        private long lastExecuted = System.currentTimeMillis();
        private boolean started = false;
        private int priority = 2;

        public RepetitiveFrameEntry(Packet _frame, long _interval,
                long _intervalStart, int _priority) {
            this.frame = _frame;
            this.interval = _interval;
            this.intervalStart = _intervalStart;
            this.priority = _priority;
        }

        public boolean shouldExecute() {
            if (System.currentTimeMillis() - lastExecuted > interval)
                return true;
            if (!started
                    && (System.currentTimeMillis() - lastExecuted > intervalStart || intervalStart == 0)) {
                started = true;
                return true;
            }
            return false;
        }
    }

    protected AutonomousAgentManager getHandlerManager() {
        return this.manager;
    }
}
