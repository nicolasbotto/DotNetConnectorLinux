/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet.metadata;

import java.util.List;

public class Argument {
	private String name;
	private String type;
	private List<Argument> fields;
	private Boolean isComplex;
	private String displayType;
	
	public void setName(String name) { this.name = name; }
	public String getName() { return this.name; }
	
	public void setType(String type) { this.type = type; }
	public String getType() { return this.type; }
	
	public List<Argument> getFields() {
		return fields;
	}
	public void setFields(List<Argument> fields) {
		this.fields = fields;
	}

	public Boolean getIsComplex() {
		return isComplex;
	}
	public void setIsComplex(Boolean isComplex) {
		this.isComplex = isComplex;
	}
	public String getDisplayType() {
		return displayType;
	}
	public void setDisplayType(String displayType) {
		this.displayType = displayType;
	}
}

