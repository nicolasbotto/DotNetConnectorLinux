/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet.connection.strategies;

import org.apache.log4j.Logger;

public abstract class BaseConnectionStrategy 
{
	protected Logger logger;
	protected ClassLoader loader;
	
	protected void log(String message)
	{
		log(message, null);
	}
	
	protected void log(String message, Throwable t)
	{
		if(this.logger != null && this.logger.isDebugEnabled())
		{
			this.logger.debug(message);
		}
	}
	
	public enum StrategyType { GAC, EXTERNAL, LEGACY }
	public abstract String getAssemblyInfo();
	public abstract StrategyType getStrategyType();
	public abstract void setLogger(Logger log);
	public abstract void setClassLoader(ClassLoader classLoader);
}
