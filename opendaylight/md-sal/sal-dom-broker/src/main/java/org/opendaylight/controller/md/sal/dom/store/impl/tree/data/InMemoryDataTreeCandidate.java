package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import com.google.common.base.Preconditions;

final class InMemoryDataTreeCandidate extends AbstractDataTreeCandidate {
	private final StoreMetadataNode newRoot;
	private final StoreMetadataNode oldRoot;

	InMemoryDataTreeCandidate(final InstanceIdentifier rootPath, final NodeModification modificationRoot,
			final StoreMetadataNode oldRoot, final StoreMetadataNode newRoot) {
		super(rootPath, modificationRoot);
		this.newRoot = Preconditions.checkNotNull(newRoot);
		this.oldRoot = Preconditions.checkNotNull(oldRoot);
	}

	@Override
	public void close() {
		// FIXME: abort operation here :)
	}

	@Override
	public StoreMetadataNode getBeforeRoot() {
		return oldRoot;
	}

	@Override
	public StoreMetadataNode getAfterRoot() {
		return newRoot;
	}
}
