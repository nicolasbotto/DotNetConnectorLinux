/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet.instrumentation;


import org.mule.api.context.notification.CustomNotificationListener;
import org.mule.api.context.notification.ServerNotification;

public class DotNetConnectorNotificationListener implements CustomNotificationListener<DotNetConnectorNotification>{

	@Override
	public void onNotification(ServerNotification notification) {
		//Received notification
	}
}
