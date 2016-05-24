/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet.jni;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

import notifications.NotificationManager;

import org.apache.log4j.Logger;
import org.mule.modules.dotnet.DotNetConnector;
import org.mule.modules.dotnet.instrumentation.DotNetNotificationManager;

public class DotNetBridge {
	private static final Logger LOGGER = Logger.getLogger(DotNetBridge.class);
	private static final DotNetNotificationManager NOTIFICATION_MANAGER = new DotNetNotificationManager();
	private static Class<?> bridgeClass;
	private static Object bridgeInstance;
	private static Method invokeNetMethod;
	private static ClassLoader loader;
	private static String connectorPrefix = "jniBridge";
	private static String connectorVersion = "1.0.0.0";
	private static boolean isInit;
	private static Method initMono;
	
	static {
		try 
		{
			String path = URLDecoder.decode(DotNetBridge.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
			log("path:" + path);
			// It's embedded in a jar, extract and redirect to new path
			if(path.endsWith(".jar"))
			{
				dataSenseSupport(path);
				path = new File(path).getParent();
			}
			
			loader = DotNetConnector.class.getClassLoader();
			
			while(loader != null)
			{
				if(loader.getClass().getName().startsWith("org.mule.module.launcher.MuleSharedDomainClassLoader"))
				{
					break;
				}
				
				if(loader.getClass().getName().startsWith("org.mule.tooling.core.classloader.MuleClassLoader"))
			    {
					if (!loader.getParent().getClass().getName().startsWith("org.mule.tooling.core.classloader.MuleClassLoader")) 
					{
						break;
					}
			    }
				
				loader = loader.getParent();
			}
			
			// For Unit test purposes
			if(loader == null)
			{
				loader = DotNetConnector.class.getClassLoader();
			}

			File myJar = new File(path + File.separator + connectorPrefix + "-" + connectorVersion + ".jar");
			URL jarURL;
		
			jarURL = myJar.toURI().toURL();
		
			Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] {URL.class});
			addUrl.setAccessible(true);
			addUrl.invoke(loader, new Object[] { jarURL });
			
			log("jarURL: "+jarURL);
			
			bridgeClass = loader.loadClass("org.mule.api.jni.Bridge");
		
			bridgeInstance = bridgeClass.newInstance();
			
			Method init = bridgeClass.getMethod("initJni", String.class);
			initMono = bridgeClass.getMethod("init", String.class);

			String jniBridgePath = path + File.separator + "JniBridge.so";
			
			init.invoke(bridgeInstance, new Object[] { jniBridgePath } );
			
			// Set the Logger
			Class<?> loggerClass = loader.loadClass("org.mule.api.jni.JniLogger");
			Object jniLogger = loggerClass.getConstructor(org.apache.log4j.Logger.class).newInstance(LOGGER);
			Method setLogger = bridgeClass.getMethod("setLogger", loggerClass);
			Object[] paramLogger = new Object[] { jniLogger };
			setLogger.invoke(bridgeInstance, paramLogger);
			
			// Set the notification manager
			Method setJniNotification = bridgeClass.getMethod("setInstrumentationManager", Object.class);
			setJniNotification.invoke(bridgeInstance, new Object[] { NOTIFICATION_MANAGER });
		
			if(bridgeClass == null)
			{
				bridgeClass = loader.loadClass("org.mule.api.jni.Bridge");
				bridgeInstance = bridgeClass.newInstance();
				
				loggerClass = loader.loadClass("org.mule.api.jni.JniLogger");
				jniLogger = loggerClass.getConstructor(org.apache.log4j.Logger.class).newInstance(LOGGER);
				setLogger = bridgeClass.getMethod("setLogger", loggerClass);
				paramLogger = new Object[] { jniLogger };
				setLogger.invoke(bridgeInstance, paramLogger);
			}
			
			invokeNetMethod = bridgeClass.getMethod("invokeNetMethod", byte[].class);
			
			initMono(path);
		} 
		catch (MalformedURLException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log("The Anypoint installation the application is running in is configured to allow the .NET Connector 1.x and this application references the .NET Connector 2.x. You must run the dotnet-version-selector utility to allow .NET Connector 2.x applications to run in this installation", e);
			try {
				throw new Exception("The Anypoint installation the application is running in is configured to allow the .NET Connector 1.x and this application references the .NET Connector 2.x. You must run the dotnet-version-selector utility to allow .NET Connector 2.x applications to run in this installation");
			} catch (Exception e1) {
				log("The Anypoint installation the application is running in is configured to allow the .NET Connector 1.x and this application references the .NET Connector 2.x. You must run the dotnet-version-selector utility to allow .NET Connector 2.x applications to run in this installation", e);
			}
		} catch (InstantiationException e) {
			log("Error initializing connector [InstantiationException]: " + e.getMessage(), e);
		} catch (IOException e) {
			log("Error extracting files: " + e.getMessage(), e);
		}
	}
	
	public static void initMono(String path)
	{
		if(!isInit)
		{
			try {
				initMono.invoke(bridgeInstance, new Object[] { path });
				isInit = true;
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void dataSenseSupport(String path) throws IOException
	{
    	File jarFile = new File(path);
    	String folder = jarFile.getParent();
    	
    	//Log("jarFile:" + jarFile);
    	//Log("folder:" + folder);
    	
    	if(jarFile.isFile()) 
    	{  
    		log(String.format("Extracting from jar file: %s", jarFile.getName()));
    		JarFile jar = new JarFile(jarFile);
    	    Enumeration<JarEntry> entries = jar.entries();
    	    while(entries.hasMoreElements()) 
    	    {
    	        JarEntry entry = entries.nextElement();
    	        String name = entry.getName();
    	        if (name.endsWith(".dll") || name.endsWith(".jar") || name.endsWith(".so") || name.endsWith(".exe"))
	        	{
    	        	String fileName = folder + File.separator + entry.getName();
    	        	File newFile = new File(fileName);
    	        	
    	        	if(!newFile.exists())
    	        	{
    	        		try(InputStream input = jar.getInputStream(entry);
	        				FileOutputStream output = new FileOutputStream(newFile))
        				{
		    				// copy file
		    				while (input.available() > 0) {
		    					output.write(input.read());
		    				}
	
		    	            log(String.format("Extracted file: %s", fileName));
    	        		}
    	        	}
    	        }
    	    }
    	    jar.close();
    	}
	}
	
	private static void log(String data)
    {
		log(data, null);
    }
	
	private static void log(String data, Throwable t) {
    	if(LOGGER.isDebugEnabled())
		{
			LOGGER.debug(data, t);
		}
    	
    	/*
    	java.util.logging.Logger logger = java.util.logging.Logger.getLogger("MyLog");  
        FileHandler fh;  

        try {  

            // This block configure the logger with handler and formatter  
            fh = new FileHandler("/home/test/datasense.log");  
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  

            // the following statement is used to log any messages  
            logger.info(data);  

        } catch (SecurityException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } */
	}
	
	public Object processRequest(Object request) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		return invokeNetMethod.invoke(bridgeInstance, new Object[] { request } );	
	}
	
	public String getTypeMetadata(String assembly, Boolean onlyDeclared, Boolean includeAutoGenerated) throws Exception
	{
		return getMetadataFromDotNet(assembly, true, true, null, null);
	}
	
	public String getMethodMetadata(String assembly, String type, String method) throws Exception
	{
		return getMetadataFromDotNet(assembly, true, true, type, method);
	}
	
	private static String getMetadataFromDotNet(String assemblyPath, boolean onlyDeclared, boolean includeAutoGenerated, String typeName, String methodData) throws Exception
    {
        try
        {
        	log("assemblyPath path: " + assemblyPath);
        	
        	if(!assemblyPath.startsWith("/"))
    		{
        		assemblyPath = "/" + assemblyPath;
    		}
        	
            String tempPath = URLDecoder.decode(DotNetBridge.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            
            if(tempPath.endsWith(".jar"))
            {
            	tempPath = new File(tempPath).getParent();
            }
            
            String metadataLoader = tempPath  + File.separator + "MetadataLoader.exe";

            log("MetadataLoader path: " + metadataLoader);
            
            ProcessBuilder pb = null;
            
            if(typeName != null)
            {
            	pb = new ProcessBuilder("mono", metadataLoader, assemblyPath, onlyDeclared+"", includeAutoGenerated+"", typeName, methodData);
            }
            else
            {
            	pb = new ProcessBuilder("mono", metadataLoader, assemblyPath, onlyDeclared+"", includeAutoGenerated+"");
            }
            
            StringBuilder sb = new StringBuilder();
            Process ps = pb.start();
            
            String command = pb.command().toString();
            log("command: " + command);
            
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream(), "UTF-8"));

            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
            }

            ps.waitFor();
            reader.close();

            String jsonData = sb.toString();
            
            log("jsonData: " + jsonData);

            if(!jsonData.isEmpty() && jsonData.startsWith("Error:"))
            {
                throw new Exception(jsonData.split(":")[1].trim());
            }
            else
            {
                return jsonData;
            }
        }
        catch (Exception e)
        {
        	 log("exception: " + e.getMessage());
            throw e;
        }
    }
}
