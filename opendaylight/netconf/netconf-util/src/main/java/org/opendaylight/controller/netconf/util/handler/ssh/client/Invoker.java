package org.opendaylight.controller.netconf.util.handler.ssh.client;

import java.io.IOException;

/**
 * Abstract class providing mechanism of invoking various SSH level services.
 * Class is not allowed to be extended, as it provides its own implementations via instance initiators.
 */
public abstract class Invoker {
    private boolean invoked = false;

    private Invoker(){}

    protected boolean isInvoked() {
        return invoked;
    }

    abstract void invoke(SshSession session) throws IOException;

    /**
     * Invoker implementation to invokes subsystem SSH service.
     *
     * @param subsystem
     * @return
     */
    public static Invoker subsystem(final String subsystem) {
        return new Invoker() {
            @Override
            void invoke(SshSession session) throws IOException {
                if (isInvoked() == true) throw new IllegalStateException("Already invoked.");

                session.startSubSystem(subsystem);
            }
        };
    }
}
