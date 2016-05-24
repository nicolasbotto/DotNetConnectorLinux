/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet.instrumentation;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Disposable;

import notifications.*;

public class DotNetNotificationManager implements NotificationManager {

	private MuleContext context;
	
	public void setContext(MuleContext context)
	{
		this.context = context;
	}
	
	@Override
	public void fireEvent(String message, Integer id) 
	{
		if(this.context.getLifecycleManager().getCurrentPhase() != Disposable.PHASE_NAME)
		{
			DotNetConnectorNotification notification = new DotNetConnectorNotification(message, id);
			this.context.getNotificationManager().fireNotification(notification);
		}
	}
}
