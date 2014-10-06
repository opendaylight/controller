/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.configpusherfeature.internal;

import java.util.LinkedHashSet;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/*
 * Wrap a Feature for the purposes of extracting the FeatureConfigSnapshotHolders from
 * its underlying ConfigFileInfo's and those of its children recursively
 *
 * Delegates the the contained feature and provides additional methods.
 */
public class ChildAwareFeatureWrapper extends AbstractFeatureWrapper implements Feature {
    private static final Logger logger = LoggerFactory.getLogger(ChildAwareFeatureWrapper.class);
    private FeaturesService featuresService= null;

    protected ChildAwareFeatureWrapper(Feature f) {
        // Don't use without a feature service
    }

    /*
     * @param f Feature to wrap
     * @param s FeaturesService to look up dependencies
     */
    ChildAwareFeatureWrapper(Feature f, FeaturesService s) throws Exception {
        super(s.getFeature(f.getName(), f.getVersion()));
        Preconditions.checkNotNull(s, "FeatureWrapper requires non-null FeatureService in constructor");
        this.featuresService = s;
    }

    protected FeaturesService getFeaturesService() {
        return featuresService;
    }

    /*
     * Get FeatureConfigSnapshotHolders appropriate to feed to the config subsystem
     * from the underlying Feature Config files and those of its children recursively
     */
    public LinkedHashSet <? extends ChildAwareFeatureWrapper> getChildFeatures() throws Exception {
        List<Dependency> dependencies = feature.getDependencies();
        LinkedHashSet <ChildAwareFeatureWrapper> childFeatures = new LinkedHashSet<ChildAwareFeatureWrapper>();
        if(dependencies != null) {
            for(Dependency dependency: dependencies) {
                Feature fi = extractFeatureFromDependency(dependency);
                if(fi != null){
                    ChildAwareFeatureWrapper wrappedFeature = new ChildAwareFeatureWrapper(fi,featuresService);
                    childFeatures.add(wrappedFeature);
                }
            }
        }
        return childFeatures;
    }

    public LinkedHashSet<FeatureConfigSnapshotHolder> getFeatureConfigSnapshotHolders() throws Exception {
        LinkedHashSet <FeatureConfigSnapshotHolder> snapShotHolders = new LinkedHashSet<FeatureConfigSnapshotHolder>();
        for(ChildAwareFeatureWrapper c: getChildFeatures()) {
            for(FeatureConfigSnapshotHolder h: c.getFeatureConfigSnapshotHolders()) {
                FeatureConfigSnapshotHolder f;
                try {
                    f = new FeatureConfigSnapshotHolder(h,this);
                    snapShotHolders.add(f);
                } catch (JAXBException e) {
                    logger.warn(
                            "Unable to parse configuration. Config from {} will be IGNORED. " +
                            "Note that subsequent config files may fail due to this problem. " +
                            "Also, this config file should have been reported already. " +
                            "Xml in this file needs to be fixed, for detailed information see enclosed exception.",
                            h.getFileInfo().getFinalname(), e);
                }
            }
        }
        snapShotHolders.addAll(super.getFeatureConfigSnapshotHolders());
        return snapShotHolders;
    }

    protected Feature extractFeatureFromDependency(Dependency dependency) throws Exception {
        Feature[] features = featuresService.listFeatures();
        VersionRange range = org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION.equals(dependency.getVersion())
                ? VersionRange.ANY_VERSION : new VersionRange(dependency.getVersion(), true, true);
        Feature fi = null;
        for(Feature f: features) {
            if (f.getName().equals(dependency.getName())) {
                Version v = VersionTable.getVersion(f.getVersion());
                if (range.contains(v)) {
                    if (fi == null || VersionTable.getVersion(fi.getVersion()).compareTo(v) < 0) {
                        fi = f;
                        break;
                    }
                }
            }
        }
        return fi;
    }

}
