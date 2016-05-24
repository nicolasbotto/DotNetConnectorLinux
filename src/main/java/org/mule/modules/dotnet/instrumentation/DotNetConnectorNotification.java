/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet.instrumentation;

import org.mule.context.notification.CustomNotification;

public class DotNetConnectorNotification extends CustomNotification {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//static final int BRIDGE_INIT_START = CUSTOM_EVENT_ACTION_START_RANGE + 1001;
	//static final int BRIDGE_INIT_STOP = CUSTOM_EVENT_ACTION_START_RANGE + 1002;
	static final int DOTNET_ARGUMENT_MAPPING_START = CUSTOM_EVENT_ACTION_START_RANGE + 1003;
	static final int DOTNET_ARGUMENT_MAPPING_STOP = CUSTOM_EVENT_ACTION_START_RANGE + 1004;
	static final int DOTNET_METHOD_START = CUSTOM_EVENT_ACTION_START_RANGE + 1005;
	static final int DOTNET_METHOD_STOP = CUSTOM_EVENT_ACTION_START_RANGE + 1006;
	
	static
	{
		//registerAction("Bridge Initialization Start", BRIDGE_INIT_START);
		//registerAction("Bridge Initialization Stop", BRIDGE_INIT_STOP);
		registerAction("DotNet Argument Mapping Start", DOTNET_ARGUMENT_MAPPING_START);
		registerAction("DotNet Argument Mapping Stop", DOTNET_ARGUMENT_MAPPING_STOP);
		registerAction("DotNet Method Execution Start", DOTNET_METHOD_START);
		registerAction("DotNet Method Execution Stop", DOTNET_METHOD_STOP);
	}
	
	public DotNetConnectorNotification(Object message, int id) {
		super(message, id);
	}
}
