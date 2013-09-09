package org.opendaylight.controller.northbound.bundlescanner.internal;

import java.util.List;

import org.opendaylight.controller.northbound.bundlescanner.IBundleScanService;
import org.osgi.framework.BundleContext;

public class BundleScanServiceImpl implements IBundleScanService {

    public BundleScanServiceImpl() {}


    @Override
    public List<Class<?>> getAnnotatedClasses(BundleContext context,
            String[] annotations,
            boolean includeDependentBundleClasses)
    {
        return BundleScanner.getInstance().getAnnotatedClasses(
                context, annotations, includeDependentBundleClasses);
    }

}
