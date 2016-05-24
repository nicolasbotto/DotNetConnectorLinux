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

@Configuration(configElementName="externalConfig", friendlyName="External Assembly")
public class ExternalStrategy extends BaseConnectionStrategy {

	@Configurable
	@Placement(group="Assembly", order = 0) 
	@Path @FriendlyName("Path") 
	@Summary("Provide the path of the .NET assembly e.g. C:\\Projects\\<Company>.<Component>.dll") 
	private String path; 
	
	@Override
	public String getAssemblyInfo() {
		try {
			return getPath();
		} catch (UnsupportedEncodingException e) {
			log(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public StrategyType getStrategyType() {
		return StrategyType.EXTERNAL;
	}
	
	public void setPath(String path)
	{
		this.path = path;
	}
	
	public String getPath() throws UnsupportedEncodingException 
	{ 
		return URLDecoder.decode(this.path, "UTF-8");
	}

	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	
	@Override
	public void setClassLoader(ClassLoader loader) {
		this.loader = loader;
	}
}