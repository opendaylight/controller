package org.opendaylight.controller.sal.utils;

/**
 * The enum which describes the generic error conditions.
 * Each enum value is associated with a minimal description string. 
 *
 */
public enum StatusCode {
	SUCCESS("Success"),

	BADREQUEST("Bad Request"),
	UNAUTHORIZED("UnAuthorized"),
	FORBIDDEN("Forbidden"),
	NOTFOUND("Not Found"),
	NOTALLOWED("Method Not Allowed"),
	NOTACCEPTABLE("Request Not Acceptable"),
	TIMEOUT("Request Timeout"),
	CONFLICT("Resource Conflict"),
	GONE("Resource Gone"),
	UNSUPPORTED("Unsupported"),

	INTERNALERROR("Internal Error"), 
	NOTIMPLEMENTED("Not Implemented"),
	NOSERVICE("Service Not Available"),
	
	UNDEFINED("Undefined Error");
	
	private String description;
	private StatusCode(String description) {
		this.description = description; 
	}
	
	/**
	 * Prints the description associated to the code value
	 */
	public String toString() {
		return description;
	}

}
