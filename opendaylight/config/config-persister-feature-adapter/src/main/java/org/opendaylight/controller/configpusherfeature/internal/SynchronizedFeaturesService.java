/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.configpusherfeature.internal;

import java.net.URI;
import java.util.EnumSet;
import java.util.Set;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;

/**
 * Karaf FeaturesService with all methods synchronized, for <a href="https://bugs.opendaylight.org/show_bug.cgi?id=6787">Bug 6787</a>.
 *
 * @author Michael Vorburger.ch
 */
public class SynchronizedFeaturesService implements FeaturesService {

    private final FeaturesService delegate;

    public SynchronizedFeaturesService(FeaturesService delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public synchronized void validateRepository(URI uri) throws Exception {
        delegate.validateRepository(uri);
    }

    @Override
    public synchronized void addRepository(URI uri) throws Exception {
        delegate.addRepository(uri);
    }

    @Override
    public synchronized void addRepository(URI uri, boolean install) throws Exception {
        delegate.addRepository(uri, install);
    }

    @Override
    public synchronized void removeRepository(URI uri) throws Exception {
        delegate.removeRepository(uri);
    }

    @Override
    public synchronized void removeRepository(URI uri, boolean uninstall) throws Exception {
        delegate.removeRepository(uri, uninstall);
    }

    @Override
    public synchronized void restoreRepository(URI uri) throws Exception {
        delegate.restoreRepository(uri);
    }

    @Override
    public synchronized Repository[] listRepositories() {
        return delegate.listRepositories();
    }

    @Override
    public synchronized Repository getRepository(String repoName) {
        return delegate.getRepository(repoName);
    }

    @Override
    public synchronized Repository getRepository(URI uri) {
        return delegate.getRepository(uri);
    }

    @Override
    public synchronized String getRepositoryName(URI uri) {
        return delegate.getRepositoryName(uri);
    }

    @Override
    public synchronized void installFeature(String name) throws Exception {
        delegate.installFeature(name);
    }

    @Override
    public synchronized void installFeature(String name, EnumSet<Option> options) throws Exception {
        delegate.installFeature(name, options);
    }

    @Override
    public synchronized void installFeature(String name, String version) throws Exception {
        delegate.installFeature(name, version);
    }

    @Override
    public synchronized void installFeature(String name, String version, EnumSet<Option> options) throws Exception {
        delegate.installFeature(name, version, options);
    }

    @Override
    public synchronized void installFeature(Feature feature, EnumSet<Option> options) throws Exception {
        delegate.installFeature(feature, options);
    }

    @Override
    public synchronized void installFeatures(Set<Feature> features, EnumSet<Option> options) throws Exception {
        delegate.installFeatures(features, options);
    }

    @Override
    public synchronized void uninstallFeature(String name, EnumSet<Option> options) throws Exception {
        delegate.uninstallFeature(name, options);
    }

    @Override
    public synchronized void uninstallFeature(String name) throws Exception {
        delegate.uninstallFeature(name);
    }

    @Override
    public synchronized void uninstallFeature(String name, String version, EnumSet<Option> options) throws Exception {
        delegate.uninstallFeature(name, version, options);
    }

    @Override
    public synchronized void uninstallFeature(String name, String version) throws Exception {
        delegate.uninstallFeature(name, version);
    }

    @Override
    public synchronized Feature[] listFeatures() throws Exception {
        return delegate.listFeatures();
    }

    @Override
    public synchronized Feature[] listInstalledFeatures() {
        return delegate.listInstalledFeatures();
    }

    @Override
    public synchronized boolean isInstalled(Feature feature) {
        return delegate.isInstalled(feature);
    }

    @Override
    public synchronized Feature[] getFeatures(String name, String version) throws Exception {
        return delegate.getFeatures(name, version);
    }

    @Override
    public synchronized Feature[] getFeatures(String name) throws Exception {
        return delegate.getFeatures(name);
    }

    @Override
    public synchronized Feature getFeature(String name, String version) throws Exception {
        return delegate.getFeature(name, version);
    }

    @Override
    public synchronized Feature getFeature(String name) throws Exception {
        return delegate.getFeature(name);
    }

    @Override
    public synchronized void refreshRepository(URI uri) throws Exception {
        delegate.refreshRepository(uri);
    }

    @Override
    public synchronized void registerListener(FeaturesListener listener) {
        delegate.registerListener(listener);
    }

    @Override
    public synchronized void unregisterListener(FeaturesListener listener) {
        delegate.unregisterListener(listener);
    }
}
