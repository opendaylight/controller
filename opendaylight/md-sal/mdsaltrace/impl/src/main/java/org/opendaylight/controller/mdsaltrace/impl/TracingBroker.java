/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.mdsaltrace.impl;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeService;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsaltrace.rev160908.Config;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * TracingBroker logs "write" operations and listener registrations to the md-sal. It logs the instance identifier path,
 * the objects themselves, as well as the stack trace of the call invoking the registration or write operation.
 * It works by operating as a "bump on the stack" between the application and actual DataBroker, intercepting write
 * and registration calls and writing to the log.
 * <h1>Wiring:</h1>
 * TracingBroker is designed to be easy to use. In fact, for bundles using Blueprint to inject their DataBroker
 * TracingBroker can be used without modifying your code at all. Simply add the dependency "mdsaltrace-features" to
 * your karaf pom:
 * <pre>
 * {@code
 *  <dependency>
 *    <groupId>org.opendaylight.controller</groupId>
 *    <artifactId>mdsaltrace-features</artifactId>
 *    <classifier>features</classifier>
 *    <type>xml</type>
 *    <scope>runtime</scope>
 *    <version>0.1.5-SNAPSHOT</version>
 *  </dependency>
 * }
 * </pre>
 * Then just load the odl-mdsaltrace feature before your feature and you're done. This works because the mdsaltrace-impl
 * bundle registers its service implementing DataBroker with a higher rank than sal-binding-broker. As such, any OSGi
 * service lookup for DataBroker will receive the TracingBroker.
 * <p/>
 * If the bundle you're using does not use plueprint to wire in its DataBroker, for instance if it gets it from the
 * ProviderContext ( {@code DataBroker dataBroker = session.getSALService(DataBroker.class);}, the easiest thing you
 * can do is simply to manually wrap the object returned by getSALService.
 * <h1>Avoiding log bloat:</h1>
 * TracingBroker can be configured to only print registrations or write ops pertaining to certain subtrees of the
 * md-sal. This can be done in the code via the methods of this class or via a config file. TracingBroker uses a more
 * convenient but non-standard representation of the instance identifiers. Each instance identifier segment's
 * class.getSimpleName() is used separated by a '/'.
 * TBD: document the config file format.
 */
public class TracingBroker  implements DataBroker, DataTreeChangeService {

    static final Logger LOG = LoggerFactory.getLogger(TracingBroker.class);

    static final private int STACK_TRACE_FIRST_RELEVANT_FRAME = 2;

    private DataBroker delegate;

    private class Watch {
        final String iidString;
        final LogicalDatastoreType store;

        public Watch(String iidString, LogicalDatastoreType storeOrNull) {
            this.store = storeOrNull;
            this.iidString = iidString;
        }

        private String toIidCompString(InstanceIdentifier iid) {
            StringBuilder builder = new StringBuilder();
            TracingBroker.toPathString(iid, builder);
            builder.append('/');
            return builder.toString();
        }

        private boolean isParent(String parent, String child) {
            return child.startsWith(parent);
        }

        public boolean subtreesOverlap(InstanceIdentifier iid, LogicalDatastoreType store, DataChangeScope scope) {
            if (this.store != null && !this.store.equals(store)) {
                return false;
            }

            String otherIidString = toIidCompString(iid);
            switch(scope) {
                case BASE:
                    return isParent(iidString, otherIidString);
                case ONE: //for now just treat like SUBTREE, even though it's not
                case SUBTREE:
                    return isParent(iidString, otherIidString) || isParent(otherIidString, iidString);
            }

            return false;
        }

        public boolean eventIsOfInterest(InstanceIdentifier iid, LogicalDatastoreType store) {
            if (this.store != null && !this.store.equals(store)) {
                return false;
            }

            return isParent(iidString, toPathString(iid));
        }
    }

    private List<Watch> registrationWatches = new ArrayList<>();
    private List<Watch> writeWatches = new ArrayList<>();

    /**
     * Ctor
     * @param delegate The real DataBroker
     * @param config config
     */
    public TracingBroker(DataBroker delegate, Config config) {
        this.delegate = delegate;
        configure(config);
    }

    /**
     * Ctor
     * @param delegate The real DataBroker
     */
    public TracingBroker(DataBroker delegate) {
        this.delegate = delegate;
    }

    private final void configure(Config config) {
        registrationWatches.clear();
        List<String> paths = config.getRegistrationWatches();
        if (paths != null) {
            for (String path : paths) {
                watchRegistrations(path, null);
            }
        }

        writeWatches.clear();
        paths = config.getWriteWatches();
        if (paths != null) {
            for (String path : paths) {
                watchWrites(path, null);
            }
        }
    }

    /**
     * Log registrations to this subtree of the md-sal
     * @param iidString the iid path of the root of the subtree
     * @param store Which LogicalDataStore? or null for both
     */
    public void watchRegistrations(String iidString, LogicalDatastoreType store) {
        registrationWatches.add(new Watch(iidString, store));
    }

    /**
     * Log writes to this subtree of the md-sal
     * @param iidString the iid path of the root of the subtree
     * @param store Which LogicalDataStore? or null for both
     */
    public void watchWrites(String iidString, LogicalDatastoreType store) {
        Watch watch = new Watch(iidString, store);
        writeWatches.add(watch);
    }

    private boolean isRegistrationWatched(InstanceIdentifier iid, LogicalDatastoreType store, DataChangeScope scope) {
        if(registrationWatches.isEmpty()) {
            return true;
        }

        for (Watch regInterest : registrationWatches) {
            if (regInterest.subtreesOverlap(iid, store, scope)) {
                return true;
            }
        }

        return false;
    }

    boolean isWriteWatched(InstanceIdentifier iid, LogicalDatastoreType store) {
        if (writeWatches.isEmpty()) {
            return true;
        }

        for (Watch watch : writeWatches) {
            if (watch.eventIsOfInterest(iid, store)) {
                return true;
            }
        }

        return false;
    }

    static void toPathString(InstanceIdentifier iid, StringBuilder builder) {
        Iterator<InstanceIdentifier.PathArgument> it = iid.getPathArguments().iterator();
        while(it.hasNext()) {
            builder.append('/').append(it.next().getType().getSimpleName());
        }
    }

    static String toPathString(InstanceIdentifier iid) {
        StringBuilder builder = new StringBuilder();
        toPathString(iid, builder);
        return builder.toString();
    }

    String getStackSummary(int shaveOffTheTop) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        StringBuilder sb = new StringBuilder();
        for(int i = STACK_TRACE_FIRST_RELEVANT_FRAME + shaveOffTheTop; i < stack.length; i++) {
            StackTraceElement frame = stack[i];
            sb.append("\n\t(TracingBroker)\t").append(frame.getClassName()).append('.').append(frame.getMethodName());
        }

        return sb.toString();
    }

    @Override
    public ReadWriteTransaction newReadWriteTransaction() {
        return new TracingReadWriteTransaction(delegate.newReadWriteTransaction(), this);
    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        return new TracingWriteTransaction(delegate.newWriteOnlyTransaction(), this);
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(
                                LogicalDatastoreType logicalDatastoreType, InstanceIdentifier<?> instanceIdentifier,
                                DataChangeListener dataChangeListener, DataChangeScope dataChangeScope) {
        if (isRegistrationWatched(instanceIdentifier, logicalDatastoreType, dataChangeScope)) {
            LOG.warn("Registration (registerDataChangeListener) for {} from {}",
                                                        toPathString(instanceIdentifier), getStackSummary(1));
        }
        return delegate.registerDataChangeListener(logicalDatastoreType, instanceIdentifier,
                                                                            dataChangeListener, dataChangeScope);
    }

    @Nonnull
    @Override
    public <T extends DataObject,L extends DataTreeChangeListener<T>> ListenerRegistration<L>
                    registerDataTreeChangeListener(@Nonnull DataTreeIdentifier<T> treeId, @Nonnull L listener) {
        if (isRegistrationWatched(treeId.getRootIdentifier(),
                                      treeId.getDatastoreType(), DataChangeScope.SUBTREE)) {
            LOG.warn("Registration (registerDataTreeChangeListener) for {} from {}",
                                    toPathString(treeId.getRootIdentifier()), getStackSummary(1));
        }
        return delegate.registerDataTreeChangeListener(treeId, listener);
    }

    @Override
    public BindingTransactionChain createTransactionChain(TransactionChainListener transactionChainListener) {
        return delegate.createTransactionChain(transactionChainListener);
    }

    @Override
    public ReadOnlyTransaction newReadOnlyTransaction() {
        return delegate.newReadOnlyTransaction();
    }

}
