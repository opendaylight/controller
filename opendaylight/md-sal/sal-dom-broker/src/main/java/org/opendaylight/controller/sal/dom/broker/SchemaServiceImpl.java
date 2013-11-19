package org.opendaylight.controller.sal.dom.broker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.Checksum;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.parser.api.YangModelParser;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.model.SchemaServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.*;

public class SchemaServiceImpl implements //
SchemaService, //
ServiceTrackerCustomizer<SchemaServiceListener, SchemaServiceListener>, //
AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SchemaServiceImpl.class);

    private ListenerRegistry<SchemaServiceListener> listeners;
    private YangModelParser parser;

    private BundleContext context;
    private BundleScanner scanner = new BundleScanner();

    /**
     * Map of currently problematic yang files that should get fixed eventually
     * after all events are received.
     */
    private final Multimap<Bundle, URL> inconsistentBundlesToYangURLs = HashMultimap.create();
    private final Multimap<Bundle, URL> consistentBundlesToYangURLs = HashMultimap.create();
    private BundleTracker<Object> bundleTracker;
    private final YangStoreCache cache = new YangStoreCache();

    private ServiceTracker<SchemaServiceListener,SchemaServiceListener> listenerTracker;

    public ListenerRegistry<SchemaServiceListener> getListeners() {
        return listeners;
    }

    public void setListeners(ListenerRegistry<SchemaServiceListener> listeners) {
        this.listeners = listeners;
    }

    public YangModelParser getParser() {
        return parser;
    }

    public void setParser(YangModelParser parser) {
        this.parser = parser;
    }

    public BundleContext getContext() {
        return context;
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    public void start() {
        checkState(parser != null);
        checkState(context != null);
        if (listeners == null) {
            listeners = new ListenerRegistry<>();
        }
        
        listenerTracker = new ServiceTracker<>(context, SchemaServiceListener.class, this);
        bundleTracker = new BundleTracker<Object>(context, BundleEvent.RESOLVED | BundleEvent.UNRESOLVED, scanner);
        bundleTracker.open();
        listenerTracker.open();
    }

    public SchemaContext getGlobalContext() {
        return getSchemaContextSnapshot();
    }

    public synchronized SchemaContext getSchemaContextSnapshot() {
        Optional<SchemaContext> yangStoreOpt = cache.getCachedSchemaContext(consistentBundlesToYangURLs);
        if (yangStoreOpt.isPresent()) {
            return yangStoreOpt.get();
        }
        SchemaContext snapshot = createSnapshot(parser, consistentBundlesToYangURLs);
        updateCache(snapshot);
        return snapshot;
    }

    @Override
    public void addModule(Module module) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public SchemaContext getSessionContext() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeModule(Module module) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    
    @Override
    public ListenerRegistration<SchemaServiceListener> registerSchemaServiceListener(SchemaServiceListener listener) {
        return listeners.register(listener);
    }
    
    @Override
    public void close() throws Exception {
        bundleTracker.close();
        // FIXME: Add listeners.close();

    }

    private synchronized boolean tryToUpdateState(Collection<URL> changedURLs, Multimap<Bundle, URL> proposedNewState,
            boolean adding) {
        Preconditions.checkArgument(changedURLs.size() > 0, "No change can occur when no URLs are changed");

        try {
            // consistent state
            // merge into
            SchemaContext snapshot = createSnapshot(parser, proposedNewState);
            consistentBundlesToYangURLs.clear();
            consistentBundlesToYangURLs.putAll(proposedNewState);
            inconsistentBundlesToYangURLs.clear();
            // update cache
            updateCache(snapshot);
            logger.info("SchemaService updated to new consistent state");
            logger.trace("SchemaService  updated to new consistent state containing {}", consistentBundlesToYangURLs);

            // notifyListeners(changedURLs, adding);
            return true;
        } catch (Exception e) {
            // inconsistent state
            logger.debug(
                    "SchemaService is falling back on last consistent state containing {}, inconsistent yang files {}, reason {}",
                    consistentBundlesToYangURLs, inconsistentBundlesToYangURLs, e.toString());
            return false;
        }
    }

    private static Collection<InputStream> fromUrlsToInputStreams(Multimap<Bundle, URL> multimap) {
        return Collections2.transform(multimap.values(), new Function<URL, InputStream>() {

            @Override
            public InputStream apply(URL url) {
                try {
                    return url.openStream();
                } catch (IOException e) {
                    logger.warn("Unable to open stream from {}", url);
                    throw new IllegalStateException("Unable to open stream from " + url, e);
                }
            }
        });
    }

    private static SchemaContext createSnapshot(YangModelParser parser, Multimap<Bundle, URL> multimap) {
        List<InputStream> models = new ArrayList<>(fromUrlsToInputStreams(multimap));
        Set<Module> modules = parser.parseYangModelsFromStreams(models);
        SchemaContext yangStoreSnapshot = parser.resolveSchemaContext(modules);
        return yangStoreSnapshot;
    }

    private void updateCache(SchemaContext snapshot) {
        cache.cacheYangStore(consistentBundlesToYangURLs, snapshot);
        
        Object[] services = listenerTracker.getServices();
        if(services != null) {
            for(Object rawListener : services) {
                SchemaServiceListener listener = (SchemaServiceListener) rawListener;
                try {
                    listener.onGlobalContextUpdated(snapshot);
                } catch (Exception e) {
                    logger.error("Exception occured during invoking listener",e);
                }
            }
        }
        for (ListenerRegistration<SchemaServiceListener> listener : listeners) {
            try {
                listener.getInstance().onGlobalContextUpdated(snapshot);
            } catch (Exception e) {
                logger.error("Exception occured during invoking listener",e);
            }
        }
    }

    private class BundleScanner implements BundleTrackerCustomizer<Object> {
        @Override
        public Object addingBundle(Bundle bundle, BundleEvent event) {

            // Ignore system bundle:
            // system bundle might have config-api on classpath &&
            // config-api contains yang files =>
            // system bundle might contain yang files from that bundle
            if (bundle.getBundleId() == 0)
                return bundle;

            Enumeration<URL> enumeration = bundle.findEntries("META-INF/yang", "*.yang", false);
            if (enumeration != null && enumeration.hasMoreElements()) {
                synchronized (this) {
                    List<URL> addedURLs = new ArrayList<>();
                    while (enumeration.hasMoreElements()) {
                        URL url = enumeration.nextElement();
                        addedURLs.add(url);
                    }
                    logger.trace("Bundle {} has event {}, bundle state {}, URLs {}", bundle, event, bundle.getState(),
                            addedURLs);
                    // test that yang store is consistent
                    Multimap<Bundle, URL> proposedNewState = HashMultimap.create(consistentBundlesToYangURLs);
                    proposedNewState.putAll(inconsistentBundlesToYangURLs);
                    proposedNewState.putAll(bundle, addedURLs);
                    boolean adding = true;
                    
                    if (tryToUpdateState(addedURLs, proposedNewState, adding) == false) {
                        inconsistentBundlesToYangURLs.putAll(bundle, addedURLs);
                    }
                }
            }
            return bundle;
        }

        @Override
        public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
            logger.debug("Modified bundle {} {} {}", bundle, event, object);
        }

        /**
         * If removing YANG files makes yang store inconsistent, method
         * {@link #getYangStoreSnapshot()} will throw exception. There is no
         * rollback.
         */

        @Override
        public synchronized void removedBundle(Bundle bundle, BundleEvent event, Object object) {
            inconsistentBundlesToYangURLs.removeAll(bundle);
            Collection<URL> consistentURLsToBeRemoved = consistentBundlesToYangURLs.removeAll(bundle);

            if (consistentURLsToBeRemoved.isEmpty()) {
                return; // no change
            }
            boolean adding = false;
            // notifyListeners(consistentURLsToBeRemoved, adding);
        }
    }

    private static final class YangStoreCache {

        Set<URL> cachedUrls;
        SchemaContext cachedContextSnapshot;

        Optional<SchemaContext> getCachedSchemaContext(Multimap<Bundle, URL> bundlesToYangURLs) {
            Set<URL> urls = setFromMultimapValues(bundlesToYangURLs);
            if (cachedUrls != null && cachedUrls.equals(urls)) {
                Preconditions.checkState(cachedContextSnapshot != null);
                return Optional.of(cachedContextSnapshot);
            }
            return Optional.absent();
        }

        private static Set<URL> setFromMultimapValues(Multimap<Bundle, URL> bundlesToYangURLs) {
            Set<URL> urls = Sets.newHashSet(bundlesToYangURLs.values());
            Preconditions.checkState(bundlesToYangURLs.size() == urls.size());
            return urls;
        }

        void cacheYangStore(Multimap<Bundle, URL> urls, SchemaContext ctx) {
            this.cachedUrls = setFromMultimapValues(urls);
            this.cachedContextSnapshot = ctx;
        }
    }
    
    @Override
    public SchemaServiceListener addingService(ServiceReference<SchemaServiceListener> reference) {
        
        SchemaServiceListener listener = context.getService(reference);
        SchemaContext _ctxContext = getGlobalContext();
        if(getContext() != null) {
            listener.onGlobalContextUpdated(_ctxContext);
        }
        return listener;
    }
    
    @Override
    public void modifiedService(ServiceReference<SchemaServiceListener> reference, SchemaServiceListener service) {
        // NOOP
    }
    
    @Override
    public void removedService(ServiceReference<SchemaServiceListener> reference, SchemaServiceListener service) {
        context.ungetService(reference);
    }
}
