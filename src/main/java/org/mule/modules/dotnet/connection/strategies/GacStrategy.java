/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet.connection.strategies;

import org.apache.log4j.Logger;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.components.Configuration;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.display.Summary;

@Configuration(configElementName = "gacConfig", friendlyName = "GAC Assembly")
public class GacStrategy extends BaseConnectionStrategy {
	
	@Configurable
	@FriendlyName("Assembly Type") 
	@Placement(group="Assembly", order = 0) 
	@Summary("Provide the Fully Qualified Type name e.g. Namespace.ClassName")
	private String type; 

	@Override
	public String getAssemblyInfo() {
		return getType();
	}
	
	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	
	@Override
	public void setClassLoader(ClassLoader loader) {
		this.loader = loader;
	}
	
	@Override
	public StrategyType getStrategyType() {
		return StrategyType.GAC;
	}
	
	public void setType(String type) { this.type = type; }
	public String getType() { return this.type; }
}