/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet.connection.strategies;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.log4j.Logger;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.components.Configuration;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Path;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.display.Summary;
import org.mule.api.annotations.param.Optional;
import org.mule.util.FilenameUtils;

@Configuration(configElementName="resourceConfig", friendlyName="Project Resource")
public class ResourceStrategy extends BaseConnectionStrategy {

	@Configurable
	@Placement(group="Assembly", order = 0) 
	@Path @FriendlyName("Path") 
	@Summary("Provide the name of the .NET assembly e.g. <Company>.<Component>.dll") 
	private String path; 
	
	@Configurable
	@Optional
	@Placement(group="Assembly", order = 0) 
	@FriendlyName("Resource folder") 
	@Summary("The name of the resource folder") 
	private String resourceFolder; 
	
	public String getResourceFolder() {
		return resourceFolder;
	}

	public void setResourceFolder(String resourceFolder) {
		this.resourceFolder = resourceFolder;
	}

	@Override
	public String getAssemblyInfo() {
		try {
			return getPath();
		} catch (UnsupportedEncodingException e) {
			log(e.getMessage(), e);
			return null;
		}
	}
	
	public void setPath(String path)
	{
		this.path = path;
	}
	
	public String getPath() throws UnsupportedEncodingException 
	{ 
		return getResourceAssemblyPath(this.path);
	}
	
	private String getResourceAssemblyPath(String assemblyPath) throws UnsupportedEncodingException
	{
		File assemblyFile = new File(assemblyPath);
		
		String assemblyResourceName = assemblyFile.getName();
		
		if(this.resourceFolder != null && !this.resourceFolder.isEmpty())
		{
			assemblyResourceName = this.resourceFolder + "/" + assemblyResourceName;
		}
				
		URL resource = loader.getResource(assemblyResourceName);
		
		if(resource == null)
		{
			log(String.format("The resource %s is not found.", assemblyFile.getName()));
			return null;
		}
		
		assemblyPath = resource.getPath();
		
		String extension = FilenameUtils.getExtension(new File(assemblyPath).getParent());
		
		if(extension.startsWith("jar"))
		{
			String baseFolder = new File(assemblyPath).getParentFile().getParent();
			assemblyPath = baseFolder + "/" + assemblyFile.getName();
		}
		
		// remove file prefix
		assemblyPath = assemblyPath.replace("file:", "");
		
		if(assemblyPath.startsWith("/") || assemblyPath.startsWith("\\"))
		{
			assemblyPath = assemblyPath.substring(1);
		}
		
		log(".NET assembly location: " + assemblyPath);
		return URLDecoder.decode(assemblyPath, "UTF-8");
	}

	@Override
	public StrategyType getStrategyType() {
		return StrategyType.EXTERNAL;
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
