package org.opendaylight.controller.sanitytest.internal;

import java.util.Timer;
import java.util.TimerTask;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevision;

public class Activator implements BundleActivator {
    //30 Second
    private static final int INITIAL_DELAY = 10000;
    private static final int SUBSEQUENT_DELAY = 1000;


    private String stateToString(int state) {
        switch (state) {
        case Bundle.ACTIVE:
            return "ACTIVE";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.UNINSTALLED:
            return "UNINSTALLED";
        case Bundle.STARTING:
            return "STARTING";
        default:
            return "Not CONVERTED: state value is " + state;
        }
    }

    public void start(final BundleContext bundleContext) throws Exception {
        Timer monitorTimer = new Timer("monitor timer", true);
        monitorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean failed = false;
                boolean resolved = false;
                while (!resolved) {
                    resolved = true;
                    failed = false;
                    for(Bundle bundle : bundleContext.getBundles()){
                        /*
                         * A bundle should be ACTIVE, unless it a fragment, in which case it should be RESOLVED
                         */
                        int state = bundle.getState();
                        if ((bundle.adapt(BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
                            //fragment
                            if (state != Bundle.RESOLVED) {
                                System.out.println("------ Failed to activate/resolve fragment = " + bundle.getSymbolicName() + " state = " + stateToString(bundle.getState()));
                                failed = true;
                                if (state == Bundle.STARTING)
                                    resolved = false;
                            }
                        } else {
                            if(state != Bundle.ACTIVE) {
                                System.out.println("------ Failed to activate/resolve bundle = " + bundle.getSymbolicName() + " state = " + stateToString(bundle.getState()));
                                failed = true;
                                if (state == Bundle.STARTING)
                                    resolved = false;
                            }
                        }
                    }
                    if (!resolved) {
                        System.out.println("all bundles haven't finished starting, will repeat");
                        try {
                            Thread.sleep(SUBSEQUENT_DELAY);
                        } catch (Exception e) {
                            ;
                        }
                    }
                }

                if(failed){
                    System.out.flush();
                    System.out.println("exiting with 1 as failed");
                    System.out.close();
                    Runtime.getRuntime().exit(1);
                } else {
                    System.out.flush();
                    System.out.println("exiting with 0 as succeeded");
                    System.out.close();
                    Runtime.getRuntime().exit(0);
                }
            }
        }, INITIAL_DELAY);
    }

    public void stop(BundleContext bundleContext) throws Exception {

    }
}
