/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet.connection.strategies;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.log4j.Logger;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.components.Configuration;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Path;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.display.Summary;
import org.mule.api.annotations.param.Optional;

@Configuration(configElementName = "config", friendlyName = "Legacy")
public class LegacyStrategy extends BaseConnectionStrategy {
	
	/**
	 * Configurable
	 */
    @Configurable
    @Optional
    @Placement(order = 0)
    @FriendlyName("Assembly Type")
    @Summary("Provide the Fully Qualified Type name e.g. Namespace.ClassName")
	private String assemblyType;
	public String getAssemblyType() { return assemblyType; }
	public void setAssemblyType(String assemblyType) { this.assemblyType = assemblyType; }
    
    /**
	 * Configurable
	 */
    @Configurable
    @Optional
    @Placement(order = 1)
    @Path
    @Summary("Provide the path of the .NET assembly e.g. C:\\Projects\\<Company>.<Component>.dll")
    private String assemblyPath;
    /**
     * Set assembly path property
     *
     * @param assemblyPath
     */
    public void setAssemblyPath(String assemblyPath) { this.assemblyPath = assemblyPath; }
    public String getAssemblyPath() 
    { 
    	try 
    	{
    		return URLDecoder.decode(this.assemblyPath, "UTF-8");
    	} 
    	catch (UnsupportedEncodingException e) 
    	{
    		log(e.getMessage(), e);
    		return null;
    	} 
	}
	private String type; 

	@Override
	public String getAssemblyInfo() {
		if(getAssemblyPath().length() == 0)
		{
			return getAssemblyType();
		}
		else
		{
			return getAssemblyPath();
		}	
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
		return StrategyType.LEGACY;
	}
	
	public void setType(String type) { this.type = type; }
	public String getType() { return this.type; }
}
