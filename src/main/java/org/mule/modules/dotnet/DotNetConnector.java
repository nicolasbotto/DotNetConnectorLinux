/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.mule.DefaultMessageCollection;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.ConnectionStrategy;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.MetaDataScope;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.display.Summary;
import org.mule.api.annotations.expressions.Lookup;
import org.mule.api.annotations.licensing.RequiresEnterpriseLicense;
import org.mule.api.annotations.lifecycle.Stop;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.MetaDataKeyParam;
import org.mule.api.annotations.param.MetaDataKeyParamAffectsType;
import org.mule.api.annotations.param.Optional;
import org.mule.api.context.MuleContextAware;
import org.mule.api.transport.PropertyScope;
import org.mule.context.notification.ListenerSubscriptionPair;
import org.mule.modules.dotnet.connection.strategies.BaseConnectionStrategy;
import org.mule.modules.dotnet.instrumentation.DotNetConnectorNotificationListener;
import org.mule.modules.dotnet.jni.DotNetBridge;
import org.mule.modules.dotnet.metadata.DotNetMetadataCategory;
import org.mule.transport.NullPayload;

/**
 * Dot Net Connector
 *
 * @author MuleSoft, Inc.
 */
@Connector(name="dotnet-connector", schemaVersion="1.0", friendlyName="DotNet Connector", minMuleVersion="3.6.1")
@RequiresEnterpriseLicense(allowEval = true)
public class DotNetConnector implements MuleContextAware
{
	private static final Logger LOGGER;
	private static final DotNetBridge DOT_NET_BRIDGE;
	
	static 
	{
		DOT_NET_BRIDGE = new DotNetBridge();
		LOGGER = Logger.getLogger(DotNetConnector.class);
	}
	
	private ObjectMapper jsonMapper = new ObjectMapper();
	private boolean isListenerRegistered = false;

	@ConnectionStrategy
	private BaseConnectionStrategy connectionStrategy;
    
    /**
     * Set scope
     *
     */
    @Configurable
    @Placement(order = 0)
    @FriendlyName("Scope")
    @Summary("Provide the scope of the module")
    private ScopeEnum scope;
    
    /**
     * Set Full Trust
     *
     */
    @Configurable
    @Placement(order = 1)
    @FriendlyName("Grant Full Trust to the .NET assembly")
    @Summary("Grant Full Trust to the .NET assembly")
    @Default("true")
    private boolean fullTrust;
    
    /**
     * Set Declared Only
     *
     */
    @Configurable
    @Placement(order = 2)
    @FriendlyName("Declared methods only")
    @Summary("Return only declared methods")
    @Default("true")
    private boolean onlyDeclared;
    
    /**
     * Set Include Auto Generated Code
     *
     */
    @Configurable
    @Placement(order = 2)
    @FriendlyName("Include auto generated code")
    @Summary("Include auto generated code by the compiler")
    @Default("false")
    private boolean includeAutoGenerated;
       
    @Lookup 
    private MuleContext muleContext; 

    public enum ScopeEnum { Singleton, Transient }
    
    /*@Stop
    public void stop() {
    	// call dotnetjni.stopserver();
    	System.out.println("Stopped connector");
    }*/
    
    @PreDestroy
    public void destroy() {
    	// call dotnetjni.stopserver();
    	System.out.println("Pre destroyed connector");
    }
    
	/**
     * Executes the dot net component
     *
     * {@sample.xml ../../../doc/mule-module-dotnet.xml.sample dotnet:execute}
     * @param event The context
     * @param methodName The name of the method to execute
     * @param parameters the data
     * @param arguments of the method
     * @param returnClass The class generated by the .Net Module
     * @throws Exception if an error occurs
     * @return Object which is the mule message
     */
	@Processor
	@MetaDataScope(DotNetMetadataCategory.class)
	public Object execute(@Optional MuleEvent event, @MetaDataKeyParam(affects=MetaDataKeyParamAffectsType.BOTH, labels = {"Type", "Method"}) @FriendlyName("Method name") @Summary("Provide the name of the .Net method that you would like to call e.g. GetProducts")String methodName, @Default("#[payload]") Object arguments)
			throws Exception 
	{
		if(methodName.contains("||")){
			methodName = methodName.split(Pattern.quote("||"))[1];
        }

		String[] parts = methodName.split(Pattern.quote("|"));
		String assemblyFQName = parts[0].trim();
		String methodToInvoke = parts[1].trim();
		
		MuleMessage msg = event.getMessage();

		if(assemblyFQName.split(",").length != 2 && assemblyFQName.split(",").length != 5)
		{
			throw new Exception("Invalid .NET Assembly Type. The .NET Assembly Type should be: fully qualified name \"Namespace.Type, AssemblyName, [Version], [Culture], [PublicKey]\"");
		}
		
		this.connectionStrategy.setClassLoader(this.muleContext.getExecutionClassLoader());
		
		String assemblyPath = this.connectionStrategy.getAssemblyInfo();
		
		Map<String, Object> args = null;
		
		if(arguments instanceof Map<?,?>)
		{
			args = (Map<String,Object>)arguments;
		}
		else
		{
			args = new HashMap<String, Object>();

			Object payload = msg.getPayload();
			
			if(msg instanceof DefaultMessageCollection)
			{
				MuleMessage[] items = ((DefaultMessageCollection)msg).getMessagesAsArray();
				
				List collectionArguments = new ArrayList();
				
				for (int i = 0; i < items.length; i++) 
				{
					collectionArguments.add(items[i].getPayload());
				}
				
				payload = collectionArguments.toArray();
			} 
			else if(payload instanceof NullPayload)
			{
				payload = "";
			}
			else if(!isByteArray(payload) && !isJson(msg) && !isXml(msg) && !isStringType(payload))
			{
				String serializedPayload = jsonMapper.writeValueAsString(msg.getPayload());
				
				payload = serializedPayload;
			}
			
			args.put("_data_", payload);
		}
		
		Object request = getRequest(assemblyPath, methodToInvoke, assemblyFQName, args, msg);
		
		Object payload = DOT_NET_BRIDGE.processRequest(request);
		
		// implement in jniBridge
		//setPropertiesFromDotNet(msg, result);

		return toPayload(payload, msg);
	}
	
	public Object toPayload(Object payload, MuleMessage msg) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		
		JniResponse response = mapper.readValue(payload.toString(), JniResponse.class);
		
		if(response.failed())
		{
			throw new Exception(response.getException());
		}
		
		switch(response.getPayload().getJni_Type())
		{
			case "NULL":
				return msg.getOriginalPayload();
			case "java.lang.String":
				String dotNetResult = response.getPayload().getJni_Value().toString();
				String contentType = "text/xml";
				
				if(isStringJsonRepresentation(dotNetResult))
				{
					contentType = "application/json";
				}
				
				msg.setProperty("Content-Type", contentType, PropertyScope.OUTBOUND);
				
				return dotNetResult;
			case "[B":
				ArrayList data = (ArrayList)response.getPayload().getJni_Value();
				byte[] returnValue = new byte[data.size()];
				
				for(int i=0; i<data.size(); i++)
				{
					returnValue[i] = ((Integer)data.get(i)).byteValue();
				}
				
				return returnValue;

			default:
				return msg.getOriginalPayload();
		}
	}

	
	public Object getRequest(String assemblyPath, String methodToInvoke, String assemblyFQName, Map<String, Object> arguments, MuleMessage msg) throws ArrayIndexOutOfBoundsException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException, JsonGenerationException, JsonMappingException, IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
        JsonGenerator jsonGenerator = new JsonFactory().createJsonGenerator(output);
        //for pretty printing
        jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter());

        jsonGenerator.writeStartObject(); // start root object

        jsonGenerator.writeStringField("assemblyName", assemblyFQName);
        jsonGenerator.writeStringField("assemblyPath", assemblyPath);
        jsonGenerator.writeStringField("methodName", methodToInvoke);
        jsonGenerator.writeBooleanField("log",getDebug());
        jsonGenerator.writeBooleanField("notifyEvents", isListenerRegistered);
        jsonGenerator.writeBooleanField("isSingleton", scope == ScopeEnum.Singleton);
        jsonGenerator.writeBooleanField("fulltrust", fullTrust);

        if (arguments != null) {
            jsonGenerator.writeFieldName("methodArguments");
            ToJsonObject(jsonGenerator, arguments);
        }

        /*for (String name : msg.getInboundPropertyNames()) {
        	jsonGenerator.writeFieldName("inboundProperties");
            ToJsonObject(jsonGenerator, msg.getInboundProperty(name));
		}
        
        for (String name : msg.getInvocationPropertyNames()) {
        	jsonGenerator.writeFieldName("invocationProperties");
        	ToJsonObject(jsonGenerator, msg.getInvocationProperty(name));
		}
        
        for (String name : msg.getOutboundPropertyNames()) {
            jsonGenerator.writeFieldName("outboundProperties");
            ToJsonObject(jsonGenerator, msg.getOutboundProperty(name));
        }

        for (String name : msg.getSessionPropertyNames()) {
            jsonGenerator.writeFieldName("sessionProperties");
            ToJsonObject(jsonGenerator, msg.getSessionProperty(name));
        }*/

        jsonGenerator.writeEndObject(); //closing root object

        jsonGenerator.flush();
        jsonGenerator.close();

        //String result = output.toString();
        byte[] data = output.toByteArray();
		return data;
	}
	
	private static Object getArgumentInJson(Object value)
	{
		String jsonFormat = "\"%s\" : %s";
		
		if(value instanceof Map<?,?>)
		{
			Map<String, Object> complex = (Map<String, Object>)value;
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			for (Map.Entry<String, Object> param : complex.entrySet())
	        {
				sb.append(String.format(jsonFormat, param.getKey(), getArgumentInJson(param.getValue())));
				sb.append(", ");
	        }

			if(sb.length() > 1)
            {
				sb = sb.delete(sb.length()-2, sb.length());
            }
			
			sb.append("}");
			value = sb.toString();
		}
		else
		{
			if(!isStringJsonRepresentation(value.toString()))
			{
				value = "\"" + value.toString() + "\"";
			}
		}
		
		return value;
	}
	
	private static void log(String data)
    {
    	if(LOGGER.isDebugEnabled())
		{
			LOGGER.debug(data);
		}

    	/*	
    	java.util.logging.Logger logger = java.util.logging.Logger.getLogger("MyLog");  
        FileHandler fh;  

        try {  

            // This block configure the logger with handler and formatter  
            fh = new FileHandler("C:/temp/datasense.log");  
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  

            // the following statement is used to log any messages  
            logger.info(data);  

        } catch (SecurityException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  */
    }
	
	private static boolean isByteArray(Object payload)
	{
		return payload instanceof byte[];
	}
	
	private static boolean isStringType(Object payload)
	{
		return payload instanceof String;
	}
	
	private static boolean isXml(MuleMessage msg)
	{
		// Check content-type
		Object contentType = msg.getProperty("Content-Type", PropertyScope.OUTBOUND);
		return contentType != null && contentType.toString().toUpperCase().contains("TEXT/XML");
	}
	
	private static boolean isJson(MuleMessage msg)
	{
		// Check content-type
		Object contentType = msg.getProperty("Content-Type", PropertyScope.OUTBOUND);
		if(contentType != null && contentType.toString().toUpperCase().contains("APPLICATION/JSON"))
		{
			return true;
		}
		
		Object payload = msg.getPayload();
		// If content is a String json representation then send as is
		if(payload instanceof String)
		{
			return isStringJsonRepresentation(payload.toString());
			/*String payloadContent = payload.toString().trim();
			
			return (payloadContent.startsWith("[") && payloadContent.endsWith("]")) ||
					payloadContent.startsWith("{") && payloadContent.endsWith("}");*/
		}

		return false;
	}
	
	private static boolean isStringJsonRepresentation(String data)
	{
		String payloadContent = data.trim();
		
		return (payloadContent.startsWith("[") && payloadContent.endsWith("]")) ||
				payloadContent.startsWith("{") && payloadContent.endsWith("}");
	}
	
	public DotNetBridge getDotNetBridge()
	{
		return DOT_NET_BRIDGE;
	}
	
	public BaseConnectionStrategy getConnectionStrategy() 
	{
		return connectionStrategy;
	}

	public void setConnectionStrategy(BaseConnectionStrategy connectionStrategy) 
	{
		this.connectionStrategy = connectionStrategy;
		this.connectionStrategy.setLogger(LOGGER);
		
		ClassLoader loader = this.muleContext == null ? DotNetConnector.class.getClassLoader() : this.muleContext.getExecutionClassLoader();
		this.connectionStrategy.setClassLoader(loader);
	}
	
	public void setFullTrust(boolean fullTrust) 
	{ 
		this.fullTrust = fullTrust; 
	}
    
	public boolean getFullTrust() 
	{ 
		return this.fullTrust; 
	}
    
    public boolean getDebug() 
    { 
    	return System.getProperty("mule.debug.enable", "false").equalsIgnoreCase("true"); 
	}
    
    public ScopeEnum getScope() 
    { 
    	return this.scope; 
	}
    
    public void setScope(ScopeEnum scope) 
    { 
    	this.scope = scope; 
	}
    
    public void setOnlyDeclared(boolean onlyDeclared) 
    { 
    	this.onlyDeclared = onlyDeclared; 
	}
    
    public boolean getOnlyDeclared() 
    { 
    	return this.onlyDeclared; 
	}
    
    public void setIncludeAutoGenerated(boolean includeAutoGenerated) 
    { 
    	this.includeAutoGenerated = includeAutoGenerated; 
	}
    
    public boolean getIncludeAutoGenerated() 
    { 
    	return this.includeAutoGenerated; 
	}
    
    public void setMuleContext(MuleContext muleContext) 
    { 
    	this.muleContext = muleContext; 
    	
    	if(this.muleContext != null)
    	{
	    	Set<ListenerSubscriptionPair> listeners = this.muleContext.getNotificationManager().getListeners();
			
	    	for (ListenerSubscriptionPair listenerSubscriptionPair : listeners) 
	    	{
				if(listenerSubscriptionPair.getListener().getClass().getSuperclass() == DotNetConnectorNotificationListener.class)
				{
					isListenerRegistered = true;
					break;
				}
			}

			//DOT_NET_BRIDGE.setNotificationManagerContext(muleContext);
    	}
    }

    private static void ToJsonObject(JsonGenerator writer, Object obj) throws IOException {

        writer.writeStartObject();

        String className = obj.getClass().getName();

        if (className.equalsIgnoreCase("java.lang.Integer")) {
            writer.writeStringField("Jni_Type", "Int32");
            writer.writeNumberField("Jni_Value", (int) obj);
        }

        if (className.equalsIgnoreCase("java.lang.String")) {
            writer.writeStringField("Jni_Type", "String");
            writer.writeStringField("Jni_Value", obj.toString());
        }

        if (className.equalsIgnoreCase("java.lang.Boolean")) {
            writer.writeStringField("Jni_Type", "Boolean");
            writer.writeBooleanField("Jni_Value", (boolean) obj);
        }

        if (className.equalsIgnoreCase("java.lang.Character")) {
            writer.writeStringField("Jni_Type", "Char");
            writer.writeFieldName("Jni_Value");
            writer.writeString(obj.toString());
        }

        if (className.equalsIgnoreCase("java.lang.Long")) {
            writer.writeStringField("Jni_Type", "Int64");
            writer.writeNumberField("Jni_Value", (long) obj);
        }

        if (className.equalsIgnoreCase("java.lang.Short")) {
            writer.writeStringField("Jni_Type", "Int16");
            writer.writeNumberField("Jni_Value", (short) obj);
        }

        if (className == "java.lang.Byte") {
            writer.writeStringField("Jni_Type", "Byte");
            writer.writeBinaryField("Jni_Value", new byte[]{(byte) obj});
        }

        if (className.equalsIgnoreCase("java.lang.Double")) {
            writer.writeStringField("Jni_Type", "Double");
            writer.writeNumberField("Jni_Value", (double) obj);
        }

        if (className.equalsIgnoreCase("java.lang.Float")) {
            writer.writeStringField("Jni_Type", "Single");
            writer.writeNumberField("Jni_Value", (float) obj);
        }

        if (className.equalsIgnoreCase("[I")) {
            writer.writeStringField("Jni_Type", "Int32[]");
            writer.writeFieldName("Jni_Value");

            int[] value = (int[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeNumber(value[i]);
            }

            writer.writeEndArray();
        }

        if (className.equalsIgnoreCase("[B")) {
            writer.writeStringField("Jni_Type", "Byte[]");
            writer.writeFieldName("Jni_Value");
            writer.writeBinary((byte[]) obj);
        }

        if (className.equalsIgnoreCase("[C")) {
            writer.writeStringField("Jni_Type", "Char[]");
            writer.writeFieldName("Jni_Value");
            writer.writeString(new String((char[]) obj));
        }

        if (className.equalsIgnoreCase("[D")) {
            writer.writeStringField("Jni_Type", "Double[]");
            writer.writeFieldName("Jni_Value");

            double[] value = (double[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeNumber(value[i]);
            }

            writer.writeEndArray();
        }

        if (className.equalsIgnoreCase("[Z")) {
            writer.writeStringField("Jni_Type", "Bool[]");
            writer.writeFieldName("Jni_Value");

            boolean[] value = (boolean[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeBoolean(value[i]);
            }

            writer.writeEndArray();
        }

        if (className.equalsIgnoreCase("[S")) {
            writer.writeStringField("Jni_Type", "Short[]");
            writer.writeFieldName("Jni_Value");

            short[] value = (short[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeNumber(value[i]);
            }

            writer.writeEndArray();
        }

        if (className.equalsIgnoreCase("[J")) {
            writer.writeStringField("Jni_Type", "Long[]");
            writer.writeFieldName("Jni_Value");

            long[] value = (long[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeNumber(value[i]);
            }

            writer.writeEndArray();
        }

        if (className.equalsIgnoreCase("[F")) {
            writer.writeStringField("Jni_Type", "Float[]");
            writer.writeFieldName("Jni_Value");

            float[] value = (float[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeNumber(value[i]);
            }

            writer.writeEndArray();
        }

        if (className.equalsIgnoreCase("[Ljava.lang.String;")) {
            writer.writeStringField("Jni_Type", "String[]");
            writer.writeFieldName("Jni_Value");

            String[] value = (String[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeString(value[i]);
            }

            writer.writeEndArray();
        }

        if (obj instanceof Map<?, ?>) {
            Map<String, Object> map = (Map<String, Object>) obj;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                writer.writeFieldName(entry.getKey());

                boolean isMap = entry.getValue() instanceof Map<?, ?>;

                /* check if it's a map to construct json object*/
                if (isMap) {
                    writer.writeStartObject();
                    writer.writeFieldName("Jni_Type");
                    writer.writeString("Map");
                    writer.writeFieldName("Jni_Value");
                }

                ToJsonObject(writer, entry.getValue());

                if (isMap) {
                    writer.writeEndObject();
                }
            }
        }

        writer.writeEndObject();
    }
}