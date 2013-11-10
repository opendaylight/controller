package org.opendaylight.controller.md.sal.common.impl.routing;

import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.concepts.Registration;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Base abstract implementation of DataReadRouter, which performs
 * a read operation on multiple data readers and then merges result.
 * 
 * @param <P>
 * @param <D>
 */
public abstract class AbstractDataReadRouter<P extends Path<?>, D> implements DataReader<P, D> {

    Multimap<P, DataReaderRegistration<P, D>> configReaders = HashMultimap.create();
    Multimap<P, DataReaderRegistration<P, D>> operationalReaders = HashMultimap.create();

    @Override
    public D readConfigurationData(P path) {
        FluentIterable<D> dataBits = FluentIterable //
                .from(getReaders(configReaders, path)).transform(configurationRead(path));
        return merge(path,dataBits);
    }

    @Override
    public D readOperationalData(P path) {
        FluentIterable<D> dataBits = FluentIterable //
                .from(getReaders(operationalReaders, path)).transform(operationalRead(path));
        return merge(path,dataBits);

    }

    /**
     * Merges data readed by reader instances from specified path
     * 
     * @param path Path on which read was performed
     * @param data Data which was returned by read operation.
     * @return Merged result.
     */
    protected abstract D merge(P path,Iterable<D> data);

    /**
     * Returns a function which performs configuration read for supplied path
     * 
     * @param path
     * @return function which performs configuration read for supplied path
     */
    
    private Function<DataReader<P, D>, D> configurationRead(final P path) {
        return new Function<DataReader<P, D>, D>() {
            @Override
            public D apply(DataReader<P, D> input) {
                return input.readConfigurationData(path);
            }
        };
    }

    /**
     * Returns a function which performs operational read for supplied path
     * 
     * @param path
     * @return function which performs operational read for supplied path
     */
    private Function<DataReader<P, D>, D> operationalRead(final P path) {
        return new Function<DataReader<P, D>, D>() {
            @Override
            public D apply(DataReader<P, D> input) {
                return input.readOperationalData(path);
            }
        };
    }

    // Registrations

    /**
     * Register's a reader for operational data.
     * 
     * @param path Path which is served by this reader
     * @param reader Reader instance which is responsible for reading particular subpath.
     * @return 
     */
    public Registration<DataReader<P, D>> registerOperationalReader(P path, DataReader<P, D> reader) {
        OperationalDataReaderRegistration<P, D> ret = new OperationalDataReaderRegistration<>(path, reader);
        operationalReaders.put(path, ret);
        return ret;
    }

    public Registration<DataReader<P, D>> registerConfigurationReader(P path, DataReader<P, D> reader) {
        ConfigurationDataReaderRegistration<P, D> ret = new ConfigurationDataReaderRegistration<>(path, reader);
        configReaders.put(path, ret);
        return ret;
    }

    Iterable<DataReader<P, D>> getOperationalReaders(P path) {
        return getReaders(operationalReaders, path);
    }

    Iterable<DataReader<P, D>> getConfigurationReaders(P path) {
        return getReaders(configReaders, path);
    }

    private Iterable<DataReader<P, D>> getReaders(Multimap<P, DataReaderRegistration<P, D>> readerMap, P path) {
        return FluentIterable
            .from(readerMap.entries()) //
            .filter(affects(path)) //
            .transform(retrieveInstance());
    }

    private void removeRegistration(OperationalDataReaderRegistration<?, ?> registration) {
        operationalReaders.remove(registration.getKey(), registration);
    }

    private void removeRegistration(ConfigurationDataReaderRegistration<?, ?> registration) {
        configReaders.remove(registration.getKey(), registration);
    }

    private Function<? super Entry<P, DataReaderRegistration<P, D>>, DataReader<P, D>> retrieveInstance() {
        return new Function<Entry<P, DataReaderRegistration<P, D>>, DataReader<P,D>>() {
            @Override
            public DataReader<P, D> apply(Entry<P, DataReaderRegistration<P, D>> input) {
                return input.getValue().getInstance();
            }
        };
    }

    private Predicate<? super Entry<P, DataReaderRegistration<P, D>>> affects(final P path) {
        
        return new Predicate<Entry<P, DataReaderRegistration<P, D>>>() {
            
            @Override
            public boolean apply(Entry<P, DataReaderRegistration<P, D>> input) {
                final Path key = input.getKey();
                return key.contains(path) || ((Path) path).contains(key);
            }
            
        };
    }

    private class ConfigurationDataReaderRegistration<P extends Path<?>, D> extends DataReaderRegistration<P, D> {

        public ConfigurationDataReaderRegistration(P key, DataReader<P, D> instance) {
            super(key, instance);
        }

        @Override
        protected void removeRegistration() {
            AbstractDataReadRouter.this.removeRegistration(this);
        }
    }

    private class OperationalDataReaderRegistration<P extends Path<?>, D> extends DataReaderRegistration<P, D> {

        public OperationalDataReaderRegistration(P key, DataReader<P, D> instance) {
            super(key, instance);
        }

        @Override
        protected void removeRegistration() {
            AbstractDataReadRouter.this.removeRegistration(this);
        }
    }

    private abstract static class DataReaderRegistration<P extends Path<?>, D> extends
            AbstractObjectRegistration<DataReader<P, D>> {

        private final P key;

        public P getKey() {
            return this.key;
        }

        public DataReaderRegistration(P key, DataReader<P, D> instance) {
            super(instance);
            this.key = key;
        }
    }
}
