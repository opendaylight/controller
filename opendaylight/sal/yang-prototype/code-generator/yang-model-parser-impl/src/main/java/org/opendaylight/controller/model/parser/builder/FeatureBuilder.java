/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.parser.builder;

import java.util.List;

import org.opendaylight.controller.model.parser.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.FeatureDefinition;
import org.opendaylight.controller.yang.model.api.SchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;


public class FeatureBuilder implements SchemaNodeBuilder {

	private final FeatureDefinitionImpl instance;
	private final QName qname;

	FeatureBuilder(QName qname) {
		this.qname = qname;
		instance = new FeatureDefinitionImpl(qname);
	}

	@Override
	public SchemaNode build() {
		return instance;
	}

	@Override
	public QName getQName() {
		return qname;
	}

	@Override
	public void setPath(SchemaPath path) {
		instance.setPath(path);
	}

	@Override
	public void setDescription(String description) {
		instance.setDescription(description);
	}

	@Override
	public void setReference(String reference) {
		instance.setReference(reference);
	}

	@Override
	public void setStatus(Status status) {
		instance.setStatus(status);
	}

	private static class FeatureDefinitionImpl implements FeatureDefinition {

		private final QName qname;
		private SchemaPath path;
		private String description;
		private String reference;
		private Status status;

		private FeatureDefinitionImpl(QName qname) {
			this.qname = qname;
		}

		@Override
		public QName getQName() {
			return qname;
		}

		@Override
		public SchemaPath getPath() {
			return path;
		}
		private void setPath(SchemaPath path) {
			this.path = path;;
		}

		@Override
		public String getDescription() {
			return description;
		}
		private void setDescription(String description) {
			this.description = description;
		}

		@Override
		public String getReference() {
			return reference;
		}
		private void setReference(String reference) {
			this.reference = reference;
		}

		@Override
		public Status getStatus() {
			return status;
		}
		private void setStatus(Status status) {
			this.status = status;
		}

		@Override
		public List<ExtensionDefinition> getExtensionSchemaNodes() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((qname == null) ? 0 : qname.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            result = prime * result + ((description == null) ? 0 : description.hashCode());
            result = prime * result + ((reference == null) ? 0 : reference.hashCode());
            result = prime * result + ((status == null) ? 0 : status.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            FeatureDefinitionImpl other = (FeatureDefinitionImpl) obj;
            if (qname == null) {
                if (other.qname != null) {
                    return false;
                }
            } else if (!qname.equals(other.qname)) {
                return false;
            }
            if (path == null) {
                if (other.path != null) {
                    return false;
                }
            } else if (!path.equals(other.path)) {
                return false;
            }
            if (description == null) {
                if (other.description != null) {
                    return false;
                }
            } else if (!description.equals(other.description)) {
                return false;
            }
            if (reference == null) {
                if (other.reference != null) {
                    return false;
                }
            } else if (!reference.equals(other.reference)) {
                return false;
            }
            if (status == null) {
                if (other.status != null) {
                    return false;
                }
            } else if (!status.equals(other.status)) {
                return false;
            }
            return true;
        }

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(FeatureDefinitionImpl.class.getSimpleName());
			sb.append("[name="+ qname);
			sb.append(", path="+ path);
			sb.append(", description="+ description);
			sb.append(", reference="+ reference);
			sb.append(", status="+ status +"]");
			return sb.toString();
		}
	}

}
