package org.opendaylight.controller.datastore.infinispan.utils;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;

/**
 * NormalizedNodeNavigator walks a {@link NormalizedNodeVisitor} through the NormalizedNode
 *
 * {@link NormalizedNode } is a tree like structure that provides a generic structure for a yang data model
 *
 * For examples of visitors
 * @see NormalizedNodePrinter
 * @see org.opendaylight.controller.datastore.infinispan.NormalizedNodeTreeCacheWriter
 *
 *
 */
public class NormalizedNodeNavigator {

        private final NormalizedNodeVisitor visitor;

        public NormalizedNodeNavigator(NormalizedNodeVisitor visitor){
            Preconditions.checkNotNull(visitor, "visitor should not be null");
            this.visitor = visitor;
        }
        public void navigate(String parentPath, NormalizedNode<?,?> normalizedNode){
            if(parentPath == null){
                parentPath = "";
            }
            navigateNormalizedNode(0, parentPath, normalizedNode);
        }

        private void navigateDataContainerNode(int level, final String parentPath, final DataContainerNode<?> dataContainerNode){
            visitor.visitNode(level, parentPath ,dataContainerNode);

            String newParentPath = parentPath + "/" + dataContainerNode.getIdentifier().toString();

            final Iterable<DataContainerChild<? extends InstanceIdentifier.PathArgument,?>> value = dataContainerNode.getValue();
            for(NormalizedNode<?,?> node : value){
                if(node instanceof MixinNode && node instanceof NormalizedNodeContainer){
                    navigateNormalizedNodeContainerMixin(level, newParentPath, (NormalizedNodeContainer<?, ?, ?>) node);
                } else {
                    navigateNormalizedNode(level, newParentPath, node);
                }
            }

        }

        private void navigateNormalizedNodeContainerMixin(int level, final String parentPath, NormalizedNodeContainer<?, ?, ?> node) {
            visitor.visitNode(level, parentPath, node);

            String newParentPath = parentPath + "/" + node.getIdentifier().toString();

            final Iterable<? extends NormalizedNode<?, ?>> value = node.getValue();
            for(NormalizedNode normalizedNode : value){
                if(normalizedNode instanceof MixinNode && normalizedNode instanceof NormalizedNodeContainer){
                    navigateNormalizedNodeContainerMixin(level + 1, newParentPath, (NormalizedNodeContainer) normalizedNode);
                } else {
                    navigateNormalizedNode(level, newParentPath, normalizedNode);
                }
            }

        }


        private void navigateNormalizedNode(int level, String parentPath, NormalizedNode<?,?> normalizedNode){
            if(normalizedNode instanceof DataContainerNode){

                final DataContainerNode<?> dataContainerNode = (DataContainerNode) normalizedNode;

                navigateDataContainerNode(level + 1, parentPath, dataContainerNode);
            } else {
                visitor.visitNode(level+1, parentPath, normalizedNode);
            }
        }
    }

