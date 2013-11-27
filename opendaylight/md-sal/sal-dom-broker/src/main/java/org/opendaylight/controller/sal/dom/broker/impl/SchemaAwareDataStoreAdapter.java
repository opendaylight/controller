package org.opendaylight.controller.sal.dom.broker.impl;

import java.awt.PageAttributes.OriginType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.impl.util.AbstractLockableDelegator;
import org.opendaylight.controller.sal.core.api.data.DataStore;
import org.opendaylight.controller.sal.core.api.model.SchemaServiceListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import static com.google.common.base.Preconditions.*;

public class SchemaAwareDataStoreAdapter extends AbstractLockableDelegator<DataStore> implements //
        DataStore, //
        SchemaServiceListener, //
        AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(SchemaAwareDataStoreAdapter.class);

    private SchemaContext schema = null;
    private boolean validationEnabled = false;
    private SchemaAwareDataMerger dataMerger = null;
    private DataReader<InstanceIdentifier, CompositeNode> reader = new MergeFirstLevelReader();

    @Override
    public boolean containsConfigurationPath(InstanceIdentifier path) {
        try {
            getDelegateReadLock().lock();
            return getDelegate().containsConfigurationPath(path);

        } finally {
            getDelegateReadLock().unlock();
        }
    }

    @Override
    public boolean containsOperationalPath(InstanceIdentifier path) {
        try {
            getDelegateReadLock().lock();
            return getDelegate().containsOperationalPath(path);

        } finally {
            getDelegateReadLock().unlock();
        }
    }

    @Override
    public Iterable<InstanceIdentifier> getStoredConfigurationPaths() {
        try {
            getDelegateReadLock().lock();
            return getDelegate().getStoredConfigurationPaths();

        } finally {
            getDelegateReadLock().unlock();
        }
    }

    @Override
    public Iterable<InstanceIdentifier> getStoredOperationalPaths() {
        try {
            getDelegateReadLock().lock();
            return getDelegate().getStoredOperationalPaths();

        } finally {
            getDelegateReadLock().unlock();
        }
    }

    @Override
    public CompositeNode readConfigurationData(InstanceIdentifier path) {
        return reader.readConfigurationData(path);
    }

    @Override
    public CompositeNode readOperationalData(InstanceIdentifier path) {
        return reader.readOperationalData(path);
    }

    @Override
    public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<InstanceIdentifier, CompositeNode> requestCommit(
            DataModification<InstanceIdentifier, CompositeNode> modification) {
        validateAgainstSchema(modification);
        DataModification<InstanceIdentifier, CompositeNode> cleanedUp = prepareMergedTransaction(modification);
        return retrieveDelegate().requestCommit(cleanedUp);
    }

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    public void setValidationEnabled(boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    private void validateAgainstSchema(DataModification<InstanceIdentifier, CompositeNode> modification) {
        if (!validationEnabled) {
            return;
        }

        if (schema == null) {
            LOG.info("Validation not performed for {}. Reason: YANG Schema not present.", modification.getIdentifier());
            return;
        }
    }

    @Override
    protected void onDelegateChanged(DataStore oldDelegate, DataStore newDelegate) {
        // NOOP
    }

    @Override
    public void onGlobalContextUpdated(SchemaContext context) {
        this.schema = context;
    }

    @Override
    public void close() throws Exception {
        this.schema = null;
    }

    private DataModification<InstanceIdentifier, CompositeNode> prepareMergedTransaction(
            DataModification<InstanceIdentifier, CompositeNode> original) {
        // NOOP for now
        return original;
    }

    private final Comparator<Entry<InstanceIdentifier, CompositeNode>> preparationComparator = new Comparator<Entry<InstanceIdentifier, CompositeNode>>() {
        @Override
        public int compare(Entry<InstanceIdentifier, CompositeNode> o1, Entry<InstanceIdentifier, CompositeNode> o2) {
            InstanceIdentifier o1Key = o1.getKey();
            InstanceIdentifier o2Key = o2.getKey();
            return Integer.compare(o1Key.getPath().size(), o2Key.getPath().size());
        }
    };

    private class MergeFirstLevelReader implements DataReader<InstanceIdentifier, CompositeNode> {

        @Override
        public CompositeNode readConfigurationData(final InstanceIdentifier path) {
            getDelegateReadLock().lock();
            try {
                if (path.getPath().isEmpty()) {
                    return null;
                }
                QName qname = null;
                CompositeNode original = getDelegate().readConfigurationData(path);
                ArrayList<Node<?>> childNodes = new ArrayList<Node<?>>();
                if (original != null) {
                    childNodes.addAll(original.getChildren());
                    qname = original.getNodeType();
                } else {
                    qname = path.getPath().get(path.getPath().size() - 1).getNodeType();
                }

                FluentIterable<InstanceIdentifier> directChildren = FluentIterable.from(getStoredConfigurationPaths())
                        .filter(new Predicate<InstanceIdentifier>() {
                            @Override
                            public boolean apply(InstanceIdentifier input) {
                                if (path.contains(input)) {
                                    int nesting = input.getPath().size() - path.getPath().size();
                                    if (nesting == 1) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        });
                for (InstanceIdentifier instanceIdentifier : directChildren) {
                    childNodes.add(getDelegate().readConfigurationData(instanceIdentifier));
                }
                if (original == null && childNodes.isEmpty()) {
                    return null;
                }

                return new CompositeNodeTOImpl(qname, null, childNodes);
            } finally {
                getDelegateReadLock().unlock();
            }
        }

        @Override
        public CompositeNode readOperationalData(final InstanceIdentifier path) {
            getDelegateReadLock().lock();
            try {
                if (path.getPath().isEmpty()) {
                    return null;
                }
                QName qname = null;
                CompositeNode original = getDelegate().readOperationalData(path);
                ArrayList<Node<?>> childNodes = new ArrayList<Node<?>>();
                if (original != null) {
                    childNodes.addAll(original.getChildren());
                    qname = original.getNodeType();
                } else {
                    qname = path.getPath().get(path.getPath().size() - 1).getNodeType();
                }

                FluentIterable<InstanceIdentifier> directChildren = FluentIterable.from(getStoredOperationalPaths())
                        .filter(new Predicate<InstanceIdentifier>() {
                            @Override
                            public boolean apply(InstanceIdentifier input) {
                                if (path.contains(input)) {
                                    int nesting = input.getPath().size() - path.getPath().size();
                                    if (nesting == 1) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        });

                for (InstanceIdentifier instanceIdentifier : directChildren) {
                    childNodes.add(getDelegate().readOperationalData(instanceIdentifier));
                }
                if (original == null && childNodes.isEmpty()) {
                    return null;
                }

                return new CompositeNodeTOImpl(qname, null, childNodes);
            } finally {
                getDelegateReadLock().unlock();
            }
        }
    }
}
