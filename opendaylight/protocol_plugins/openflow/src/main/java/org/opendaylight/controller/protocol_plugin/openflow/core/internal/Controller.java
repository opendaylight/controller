
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.core.internal;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.IMessageListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitchStateListener;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.util.HexString;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Controller implements IController, CommandProvider {
    private static final Logger logger = LoggerFactory
            .getLogger(Controller.class);
    private ControllerIO controllerIO;
    private Thread switchEventThread;
    private ConcurrentHashMap<Long, ISwitch> switches;
    private BlockingQueue<SwitchEvent> switchEvents;
    // only 1 message listener per OFType
    private ConcurrentMap<OFType, IMessageListener> messageListeners;
    // only 1 switch state listener
    private ISwitchStateListener switchStateListener;
    private AtomicInteger switchInstanceNumber;

    /*
     * this thread monitors the switchEvents queue for new incoming events from switch
     */
    private class EventHandler implements Runnable {
        @Override
        public void run() {

            while (true) {
                try {
                    SwitchEvent ev = switchEvents.take();
                    SwitchEvent.SwitchEventType eType = ev.getEventType();
                    ISwitch sw = ev.getSwitch();
                    if (eType != SwitchEvent.SwitchEventType.SWITCH_MESSAGE) {
                        //logger.debug("Received " + ev.toString() + " from " + sw.toString());
                    }
                    switch (eType) {
                    case SWITCH_ADD:
                        Long sid = sw.getId();
                        ISwitch existingSwitch = switches.get(sid);
                        if (existingSwitch != null) {
                            logger.info(" Replacing existing "
                                    + existingSwitch.toString() + " with New "
                                    + sw.toString());
                            disconnectSwitch(existingSwitch);
                        }
                        switches.put(sid, sw);
                        notifySwitchAdded(sw);
                        break;
                    case SWITCH_DELETE:
                        disconnectSwitch(sw);
                        break;
                    case SWITCH_ERROR:
                        disconnectSwitch(sw);
                        break;
                    case SWITCH_MESSAGE:
                        OFMessage msg = ev.getMsg();
                        if (msg != null) {
                            IMessageListener listener = messageListeners
                                    .get(msg.getType());
                            if (listener != null) {
                                listener.receive(sw, msg);
                            }
                        }
                        break;
                    default:
                        logger.error("unknow switch event " + eType.ordinal());
                    }
                } catch (InterruptedException e) {
                    switchEvents.clear();
                    return;
                }
            }
        }

    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    public void init() {
        logger.debug("OpenFlowCore init");
        this.switches = new ConcurrentHashMap<Long, ISwitch>();
        this.switchEvents = new LinkedBlockingQueue<SwitchEvent>();
        this.messageListeners = new ConcurrentHashMap<OFType, IMessageListener>();
        this.switchStateListener = null;
        this.switchInstanceNumber = new AtomicInteger(0);
        registerWithOSGIConsole();

    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    public void start() {
        logger.debug("OpenFlowCore start() is called");
        /*
         * start a thread to handle event coming from the switch
         */
        switchEventThread = new Thread(new EventHandler(), "SwitchEvent Thread");
        switchEventThread.start();

        // spawn a thread to start to listen on the open flow port
        controllerIO = new ControllerIO(this);
        try {
            controllerIO.start();
        } catch (IOException ex) {
            logger.error("Caught exception: " + ex + " during start");
        }
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    public void stop() {
        for (Iterator<Entry<Long, ISwitch>> it = switches.entrySet().iterator(); it
                .hasNext();) {
            Entry<Long, ISwitch> entry = it.next();
            ((SwitchHandler) entry.getValue()).stop();
            it.remove();
        }
        switchEventThread.interrupt();
        try {
        	controllerIO.shutDown();
        } catch (IOException ex) {
        	logger.error("Caught exception: " + ex + " during stop");
        }
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    public void destroy() {
    }

    @Override
    public void addMessageListener(OFType type, IMessageListener listener) {
        IMessageListener currentListener = this.messageListeners.get(type);
        if (currentListener != null) {
            logger.warn(type.toString() + " already listened by "
                    + currentListener.toString());
        }
        this.messageListeners.put(type, listener);
        logger.debug(type.toString() + " is now listened by "
                + listener.toString());
    }

    @Override
    public void removeMessageListener(OFType type, IMessageListener listener) {
        IMessageListener currentListener = this.messageListeners.get(type);
        if ((currentListener != null) && (currentListener == listener)) {
            this.messageListeners.remove(type);
        }
    }

    @Override
    public void addSwitchStateListener(ISwitchStateListener listener) {
        if (this.switchStateListener != null) {
            logger.warn(this.switchStateListener.toString()
                    + "already listened to switch events");
        }
        this.switchStateListener = listener;
        logger.debug(listener.toString() + " now listens to switch events");
    }

    @Override
    public void removeSwitchStateListener(ISwitchStateListener listener) {
        if ((this.switchStateListener != null)
                && (this.switchStateListener == listener)) {
            this.switchStateListener = null;
        }
    }

    public void handleNewConnection(Selector selector,
            SelectionKey serverSelectionKey) {
        ServerSocketChannel ssc = (ServerSocketChannel) serverSelectionKey
                .channel();
        SocketChannel sc = null;
        try {
            sc = ssc.accept();
            // create new switch
            int i = this.switchInstanceNumber.addAndGet(1);
            String instanceName = "SwitchHandler-" + i;
            SwitchHandler switchHandler = new SwitchHandler(this, sc,
                    instanceName);
            switchHandler.start();
            logger.info(instanceName + " connected: " + sc.toString());
        } catch (IOException e) {
            return;
        }
    }

    private void disconnectSwitch(ISwitch sw) {
        if (((SwitchHandler) sw).isOperational()) {
            Long sid = sw.getId();
            if (this.switches.remove(sid, sw)) {
                logger.warn(sw.toString() + " is disconnected");
                notifySwitchDeleted(sw);
            } else {
                //logger.warn(sw.toString() + " has been replaced by " +
                //	this.switches.get(sid));
            }
        }
        ((SwitchHandler) sw).stop();
        sw = null;
    }

    private void notifySwitchAdded(ISwitch sw) {
        if (switchStateListener != null) {
            switchStateListener.switchAdded(sw);
        }
    }

    private void notifySwitchDeleted(ISwitch sw) {
        if (switchStateListener != null) {
            switchStateListener.switchDeleted(sw);
        }
    }

    private synchronized void addSwitchEvent(SwitchEvent event) {
        try {
            this.switchEvents.put(event);
        } catch (InterruptedException e) {
            logger.debug("SwitchEvent caught Interrupt Exception");
        }
    }

    public void takeSwtichEventAdd(ISwitch sw) {
        SwitchEvent ev = new SwitchEvent(
                SwitchEvent.SwitchEventType.SWITCH_ADD, sw, null);
        addSwitchEvent(ev);
    }

    public void takeSwitchEventDelete(ISwitch sw) {
        SwitchEvent ev = new SwitchEvent(
                SwitchEvent.SwitchEventType.SWITCH_DELETE, sw, null);
        addSwitchEvent(ev);
    }

    public void takeSwitchEventError(ISwitch sw) {
        SwitchEvent ev = new SwitchEvent(
                SwitchEvent.SwitchEventType.SWITCH_ERROR, sw, null);
        addSwitchEvent(ev);
    }

    public void takeSwitchEventMsg(ISwitch sw, OFMessage msg) {
        if (messageListeners.get(msg.getType()) != null) {
            SwitchEvent ev = new SwitchEvent(
                    SwitchEvent.SwitchEventType.SWITCH_MESSAGE, sw, msg);
            addSwitchEvent(ev);
        }
    }

    @Override
    public Map<Long, ISwitch> getSwitches() {
        return this.switches;
    }

    @Override
    public ISwitch getSwitch(Long switchId) {
        return this.switches.get(switchId);
    }

    public void _controllerShowSwitches(CommandInterpreter ci) {
        Set<Long> sids = switches.keySet();
        StringBuffer s = new StringBuffer();
        int size = sids.size();
        if (size == 0) {
            ci.print("switches: empty");
            return;
        }
        Iterator<Long> iter = sids.iterator();
        s.append("Total: " + size + " switches\n");
        while (iter.hasNext()) {
            Long sid = iter.next();
            Date date = switches.get(sid).getConnectedDate();
            String switchInstanceName = ((SwitchHandler) switches.get(sid)).getInstanceName();
            s.append(switchInstanceName + "/" + HexString.toHexString(sid)
                    + " connected since " + date.toString() + "\n");
        }
        ci.print(s.toString());
        return;
    }

    public void _controllerReset(CommandInterpreter ci) {
        ci.print("...Disconnecting the communication to all switches...\n");
        stop();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
        } finally {
            ci.print("...start to accept connections from switches...\n");
            start();
        }
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("--Open Flow Controller --\n");
        help.append("\tcontrollerShowSwitches\n");
        help.append("\tcontrollerReset\n");
        return help.toString();
    }
}
