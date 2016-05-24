/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet.metadata;

import java.util.List;

public class TypeMetadata {

	private String name;
	private List<MethodMetadata> methods;
	private String fullName;
	
	public void setFullName(String fullName) { this.fullName = fullName; }
	public String getFullName() { return this.fullName; }
	
	public void setName(String name) { this.name = name; }
	public String getName() { return this.name; }

	public List<MethodMetadata> getMethods() {
		return methods;
	}
	public void setMethods(List<MethodMetadata> methods) {
		this.methods = methods;
	}
}