/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.dom.impl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.trace.api.TracingDOMDataBroker;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTrackedRegistry;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.mdsaltrace.rev160908.Config;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:JavadocStyle")
//...because otherwise it whines about the elements in the @code block even though it's completely valid Javadoc

/**
 * TracingBroker logs "write" operations and listener registrations to the md-sal. It logs the instance identifier path,
 * the objects themselves, as well as the stack trace of the call invoking the registration or write operation.
 * It works by operating as a "bump on the stack" between the application and actual DataBroker, intercepting write
 * and registration calls and writing to the log.
 *
 * <p>In addition, it (optionally) can also keep track of the stack trace of all new transaction allocations
 * (including TransactionChains, and transactions created in turn from them), in order to detect and report leaks
 *  results from transactions which were not closed.
 *
 * <h1>Wiring:</h1>
 * TracingBroker is designed to be easy to use. In fact, for bundles using Blueprint to inject their DataBroker
 * TracingBroker can be used without modifying your code at all in two simple steps:
 * <ol>
 * <li>
 * Simply add the dependency "mdsaltrace-features" to
 * your Karaf pom:
 * <pre>
 * {@code
 *  <dependency>
 *    <groupId>org.opendaylight.controller</groupId>
 *    <artifactId>mdsal-trace-features</artifactId>
 *    <classifier>features</classifier>
 *    <type>xml</type>
 *    <scope>runtime</scope>
 *    <version>0.1.6-SNAPSHOT</version>
 *  </dependency>
 * }
 * </pre>
 * </li>
 * <li>
 * Then just load the odl-mdsal-trace feature before your feature and you're done.
 * </li>
 * </ol>
 * This works because the mdsaltrace-impl bundle registers its service implementing DOMDataBroker with a higher
 * rank than sal-binding-broker. As such, any OSGi service lookup for DataBroker will receive the TracingBroker.
 * <p> </p>
 * <h1>Avoiding log bloat:</h1>
 * TracingBroker can be configured to only print registrations or write ops pertaining to certain subtrees of the
 * md-sal. This can be done in the code via the methods of this class or via a config file. TracingBroker uses a more
 * convenient but non-standard representation of the instance identifiers. Each instance identifier segment's
 * class.getSimpleName() is used separated by a '/'.
 * <p> </p>
 * <h1>Known issues</h1>
 * <ul>
 *     <li>
 *        Filtering by paths. For some registrations the codec that converts back from the DOM to binding paths is
 *        busted. As such, an aproximated path is used in the output. For now it is recommended not to use
 *        watchRegistrations and allow all registrations to be logged.
 *     </li>
 * </ul>
 *
 */
public class TracingBroker implements TracingDOMDataBroker {

    static final Logger LOG = LoggerFactory.getLogger(TracingBroker.class);

    private static final int STACK_TRACE_FIRST_RELEVANT_FRAME = 2;

    private final BindingNormalizedNodeSerializer codec;
    private final DOMDataBroker delegate;
    private final List<Watch> registrationWatches = new ArrayList<>();
    private final List<Watch> writeWatches = new ArrayList<>();

    private final boolean isDebugging;
    private final CloseTrackedRegistry<TracingTransactionChain> transactionChainsRegistry;
    private final CloseTrackedRegistry<TracingReadOnlyTransaction> readOnlyTransactionsRegistry;
    private final CloseTrackedRegistry<TracingWriteTransaction> writeTransactionsRegistry;
    private final CloseTrackedRegistry<TracingReadWriteTransaction> readWriteTransactionsRegistry;

    private class Watch {
        final String iidString;
        final LogicalDatastoreType store;

        Watch(String iidString, LogicalDatastoreType storeOrNull) {
            this.store = storeOrNull;
            this.iidString = iidString;
        }

        private String toIidCompString(YangInstanceIdentifier iid) {
            StringBuilder builder = new StringBuilder();
            toPathString(iid, builder);
            builder.append('/');
            return builder.toString();
        }

        private boolean isParent(String parent, String child) {
            int parentOffset = 0;
            if (parent.length() > 0 && parent.charAt(0) == '<') {
                parentOffset = parent.indexOf('>') + 1;
            }

            int childOffset = 0;
            if (child.length() > 0 && child.charAt(0) == '<') {
                childOffset = child.indexOf('>') + 1;
            }

            return child.startsWith(parent.substring(parentOffset), childOffset);
        }

        public boolean subtreesOverlap(YangInstanceIdentifier iid, LogicalDatastoreType store,
                                                                AsyncDataBroker.DataChangeScope scope) {
            if (this.store != null && !this.store.equals(store)) {
                return false;
            }

            String otherIidString = toIidCompString(iid);
            switch (scope) {
                case BASE:
                    return isParent(iidString, otherIidString);
                case ONE: //for now just treat like SUBTREE, even though it's not
                case SUBTREE:
                    return isParent(iidString, otherIidString) || isParent(otherIidString, iidString);
                default:
                    return false;
            }
        }

        public boolean eventIsOfInterest(YangInstanceIdentifier iid, LogicalDatastoreType store) {
            if (this.store != null && !this.store.equals(store)) {
                return false;
            }

            return isParent(iidString, toPathString(iid));
        }
    }

    public TracingBroker(DOMDataBroker delegate, Config config, BindingNormalizedNodeSerializer codec) {
        this.delegate = Objects.requireNonNull(delegate);
        this.codec = Objects.requireNonNull(codec);
        configure(config);

        if (config.isTransactionDebugContextEnabled() != null) {
            this.isDebugging = config.isTransactionDebugContextEnabled();
        } else {
            this.isDebugging = false;
        }
        this.transactionChainsRegistry     = new CloseTrackedRegistry<>(this, "createTransactionChain", isDebugging);
        this.readOnlyTransactionsRegistry  = new CloseTrackedRegistry<>(this, "newReadOnlyTransaction", isDebugging);
        this.writeTransactionsRegistry     = new CloseTrackedRegistry<>(this, "newWriteOnlyTransaction", isDebugging);
        this.readWriteTransactionsRegistry = new CloseTrackedRegistry<>(this, "newReadWriteTransaction", isDebugging);
    }

    private void configure(Config config) {
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
     * Log registrations to this subtree of the md-sal.
     * @param iidString the iid path of the root of the subtree
     * @param store Which LogicalDataStore? or null for both
     */
    public void watchRegistrations(String iidString, LogicalDatastoreType store) {
        LOG.info("Watching registrations to {} in {}", iidString, store);
        registrationWatches.add(new Watch(iidString, store));
    }

    /**
     * Log writes to this subtree of the md-sal.
     * @param iidString the iid path of the root of the subtree
     * @param store Which LogicalDataStore? or null for both
     */
    public void watchWrites(String iidString, LogicalDatastoreType store) {
        LOG.info("Watching writes to {} in {}", iidString, store);
        Watch watch = new Watch(iidString, store);
        writeWatches.add(watch);
    }

    private boolean isRegistrationWatched(YangInstanceIdentifier iid,
                                                            LogicalDatastoreType store, DataChangeScope scope) {
        if (registrationWatches.isEmpty()) {
            return true;
        }

        for (Watch regInterest : registrationWatches) {
            if (regInterest.subtreesOverlap(iid, store, scope)) {
                return true;
            }
        }

        return false;
    }

    boolean isWriteWatched(YangInstanceIdentifier iid, LogicalDatastoreType store) {
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

    static void toPathString(InstanceIdentifier<? extends DataObject> iid, StringBuilder builder) {
        for (InstanceIdentifier.PathArgument pathArg : iid.getPathArguments()) {
            builder.append('/').append(pathArg.getType().getSimpleName());
        }
    }

    String toPathString(YangInstanceIdentifier  yiid) {
        StringBuilder sb = new StringBuilder();
        toPathString(yiid, sb);
        return sb.toString();
    }


    private void toPathString(YangInstanceIdentifier yiid, StringBuilder sb) {
        InstanceIdentifier<?> iid = codec.fromYangInstanceIdentifier(yiid);
        if (null == iid) {
            reconstructIidPathString(yiid, sb);
        } else {
            toPathString(iid, sb);
        }
    }

    private void reconstructIidPathString(YangInstanceIdentifier yiid, StringBuilder sb) {
        sb.append("<RECONSTRUCTED FROM: \"").append(yiid.toString()).append("\">");
        for (YangInstanceIdentifier.PathArgument pathArg : yiid.getPathArguments()) {
            if (pathArg instanceof YangInstanceIdentifier.AugmentationIdentifier) {
                sb.append('/').append("AUGMENTATION");
                continue;
            }
            sb.append('/').append(pathArg.getNodeType().getLocalName());
        }
    }

    String getStackSummary() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        StringBuilder sb = new StringBuilder();
        for (int i = STACK_TRACE_FIRST_RELEVANT_FRAME; i < stack.length; i++) {
            StackTraceElement frame = stack[i];
            sb.append("\n\t(TracingBroker)\t").append(frame.getClassName()).append('.').append(frame.getMethodName());
        }

        return sb.toString();
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return new TracingReadWriteTransaction(delegate.newReadWriteTransaction(), this, readWriteTransactionsRegistry);
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return new TracingWriteTransaction(delegate.newWriteOnlyTransaction(), this, writeTransactionsRegistry);
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(
                                                        LogicalDatastoreType store, YangInstanceIdentifier yiid,
                                                        DOMDataChangeListener listener, DataChangeScope scope) {
        if (isRegistrationWatched(yiid, store, scope)) {
            LOG.warn("Registration (registerDataChangeListener) for {} from {}",
                    toPathString(yiid), getStackSummary());
        }
        return delegate.registerDataChangeListener(store, yiid, listener, scope);
    }

    @Override
    public DOMTransactionChain createTransactionChain(TransactionChainListener transactionChainListener) {
        return new TracingTransactionChain(
                delegate.createTransactionChain(transactionChainListener), this, transactionChainsRegistry);
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return new TracingReadOnlyTransaction(delegate.newReadOnlyTransaction(), this, readOnlyTransactionsRegistry);
    }

    @Nonnull
    @Override
    public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> res = delegate.getSupportedExtensions();
        DOMDataTreeChangeService treeChangeSvc = (DOMDataTreeChangeService) res.get(DOMDataTreeChangeService.class);
        if (treeChangeSvc == null) {
            return res;
        }

        res = new HashMap<>(res);

        res.put(DOMDataTreeChangeService.class, new DOMDataTreeChangeService() {
            @Nonnull
            @Override
            public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerDataTreeChangeListener(
                    @Nonnull DOMDataTreeIdentifier domDataTreeIdentifier, @Nonnull L listener) {
                if (isRegistrationWatched(domDataTreeIdentifier.getRootIdentifier(),
                        domDataTreeIdentifier.getDatastoreType(), DataChangeScope.SUBTREE)) {
                    LOG.warn("Registration (registerDataTreeChangeListener) for {} from {}",
                            toPathString(domDataTreeIdentifier.getRootIdentifier()), getStackSummary());
                }
                return treeChangeSvc.registerDataTreeChangeListener(domDataTreeIdentifier, listener);
            }
        });

        return res;
    }

    public boolean printOpenTransactions(PrintStream ps) {
        if (transactionChainsRegistry.getAllUnique().isEmpty()
            && readOnlyTransactionsRegistry.getAllUnique().isEmpty()
            && writeTransactionsRegistry.getAllUnique().isEmpty()
            && readWriteTransactionsRegistry.getAllUnique().isEmpty()) {

            return false;
        }

        ps.println(getClass().getSimpleName() + " found not yet (or never..) closed transaction[chain]s");
        ps.println();
        printRegistryOpenTransactions(readOnlyTransactionsRegistry, ps, "");
        printRegistryOpenTransactions(writeTransactionsRegistry, ps, "");
        printRegistryOpenTransactions(readWriteTransactionsRegistry, ps, "");

        // Now print details for each non-closed TransactionChain
        // incl. in turn each ones own read/Write[Only]TransactionsRegistry
        ps.println(transactionChainsRegistry.getCreateDescription());
        transactionChainsRegistry.getAllUnique().forEach(entry -> {
            ps.println("  " + entry.getNumberAddedNotRemoved() + "x TransactionChains opened, which are not closed:");
            entry.getStackTraceElements().forEach(line -> ps.println("    " + line));
            @SuppressWarnings("resource")
            TracingTransactionChain txChain = (TracingTransactionChain) entry
                .getExampleCloseTracked().getRealCloseTracked();
            printRegistryOpenTransactions(txChain.getReadOnlyTransactionsRegistry(), ps, "    ");
            printRegistryOpenTransactions(txChain.getWriteTransactionsRegistry(), ps, "    ");
            printRegistryOpenTransactions(txChain.getReadWriteTransactionsRegistry(), ps, "    ");
        });
        ps.println();

        return true;
    }

    private void printRegistryOpenTransactions(CloseTrackedRegistry<?> registry, PrintStream ps, String indent) {
        ps.println(indent + registry.getCreateDescription());
        registry.getAllUnique().forEach(entry -> {
            ps.println(indent + "  " + entry.getNumberAddedNotRemoved()
                + "x transactions opened here, which are not closed:");
            entry.getStackTraceElements().forEach(line -> ps.print("    " + line));
        });
        ps.println();
    }
}
