/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved. 
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

/**
 * Represents the return object of the osgi service interfaces function calls.
 * It contains a code {@code StatusCode} representing the result of the call
 * and a string which describes a failure reason (if any) in human readable form.
 */
public class Status {
	StatusCode code;
	String description;
	
	/**
	 * Generates an instance of the Status class.
	 * 
	 * @param errorCode The status code. If passed as null, code will be 
	 * stored as {@code StatusCode.UNDEFINED}
	 * @param description The human readable description of the status. If passed
	 * as null, description will be inferred by the code
	 */
	public Status(StatusCode errorCode, String description) {
		this.code = (errorCode != null)? errorCode : StatusCode.UNDEFINED;
		this.description = (description != null)? description : this.code.toString();
	}
	
	/**
	 * Returns the status code
	 * @return the {@code StatusCode} representing the status code 
	 */
	public StatusCode getCode() {
		return code;
	}
	
	/**
	 * Returns a human readable description of the failure if any
	 * @return a string representing the reason of failure
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Tells whether the status is successful
	 * @return true if the Status code is {@code StatusCode.SUCCESS}
	 */
	public boolean isSuccess() {
		return code == StatusCode.SUCCESS;
	}
	
	@Override
	public String toString() {
		return code + ": " + description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Status other = (Status) obj;
		if (code != other.code)
			return false;
		return true;
	}
}
