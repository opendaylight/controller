package org.opendaylight.controller.sanitytest.internal;

import org.osgi.framework.*;

import java.util.Timer;
import java.util.TimerTask;

public class Activator implements BundleActivator {
    //30 Second
    private static final int DELAY = 30000;


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
        default:
            return "Not CONVERTED";
        }
    }

    public void start(final BundleContext bundleContext) throws Exception {
        Timer monitorTimer = new Timer("monitor timer", true);
        monitorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean failed = false;
                for(Bundle bundle : bundleContext.getBundles()){
                    if(bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.RESOLVED) {
                        System.out.println("------ Failed to activate/resolve bundle = " + bundle.getSymbolicName() + " state = " + stateToString(bundle.getState()));
                        failed = true;
                    }
                }

                if(failed){
                    System.exit(-1);
                } else {
                    System.exit(0);
                }
            }
        }, DELAY);
    }

    public void stop(BundleContext bundleContext) throws Exception {

    }
}
