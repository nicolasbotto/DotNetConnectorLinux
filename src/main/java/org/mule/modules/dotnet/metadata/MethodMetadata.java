/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet.metadata;

import java.util.List;

public class MethodMetadata {

	private String name;
	private List<Argument> parameters;
	private Argument returnValue;
	private String typeName;
	
	public void setName(String name) { this.name = name; }
	public String getName() { return this.name; }
	
	public void setTypeName(String typeName) { this.name = typeName; }
	public String getTypeName() { return this.typeName; }

	public List<Argument> getParameters() {
		return parameters;
	}
	public void setParameters(List<Argument> parameters) {
		this.parameters = parameters;
	}

	public Argument getReturnValue() {
		return returnValue;
	}
	public void setReturnValue(Argument returnValue) {
		this.returnValue = returnValue;
	}
}
