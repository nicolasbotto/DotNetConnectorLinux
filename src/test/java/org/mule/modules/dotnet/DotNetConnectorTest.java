/**
 * (c) 2003-2014 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.dotnet;

import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.security.Credentials;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.api.transport.ReplyToHandler;
import org.mule.common.metadata.DefaultDefinedMapMetaDataModel;
import org.mule.common.metadata.DefaultSimpleMetaDataModel;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataKey;
import org.mule.devkit.api.metadata.ComposedMetaDataKey;
import org.mule.devkit.api.metadata.ComposedMetaDataKeyBuilder;
import org.mule.devkit.api.metadata.ComposedMetaDataKeyBuilder.CombinationBuilder;
import org.mule.management.stats.ProcessingTime;
import org.mule.modules.dotnet.connection.strategies.ResourceStrategy;
import org.mule.modules.dotnet.metadata.DotNetMetadataCategory;

import sun.reflect.generics.tree.FieldTypeSignature;

import com.google.inject.Key;

public class DotNetConnectorTest
{/*
	@Test
	
	public void testOutOfProcess() throws Exception
	{
		DotNetConnector myConnector = new DotNetConnector();
		
		ResourceStrategy strategy = new ResourceStrategy();
		strategy.setPath("Test.SampleComponent.dll");
		myConnector.setConnectionStrategy(strategy);
		myConnector.setOnlyDeclared(true);
		
		Map<String, Object> methodArgs = new LinkedHashMap<String, Object>();

        methodArgs.put("person", "{ \"name\" : \"nicolas\", \"lastName\" : \"foo\", \"id\" : 432, \"myRide\" : { \"Id\" : 1, \"Year\" : \"1990\", \"TopSpeed\" : \"330.50\", \"Model\" : \"Coupe\", \"Brand\" : \"GM\", \"ExteriorColor\" : { \"Name\" : \"red\", \"RGB\" : \"123,220,213\" }}}");
        methodArgs.put("increment", true);
        methodArgs.put("id", 87);
		
        //String methodName = "Test.SampleComponent.Sample, Test.SampleComponent, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null||Test.SampleComponent.Sample, Test.SampleComponent, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null | ExecuteComplexMix(Test.SampleComponent.Person person, System.Int32 id, System.Boolean increment) -&gt; System.Object";
        String methodName = "ExecuteComplexMix(Test.SampleComponent.Person person, System.Int32 id, System.Boolean increment) -&gt; System.Object";
        
        Object request = myConnector.getRequest("/home/test/Documents/muleflow/target/test-classes/Test.SampleComponent.dll", methodName, "Test.SampleComponent.Sample, Test.SampleComponent", methodArgs, null);
        Object result = myConnector.getDotNetBridge().processRequest(request);
		
		Assert.assertNotNull(result);
	}
	
	@Test
	public void testOutOfProcessWithByteArray() throws Exception
	{
		DotNetConnector myConnector = new DotNetConnector();
		
		ResourceStrategy strategy = new ResourceStrategy();
		strategy.setPath("Test.SampleComponent.dll");
		myConnector.setConnectionStrategy(strategy);
		myConnector.setOnlyDeclared(true);
		
		Map<String, Object> methodArgs = new LinkedHashMap<String, Object>();

		String input = "Hello from J@va";
		
        methodArgs.put("data", input.getBytes());
		
        String methodName = "GetBytes(System.Byte[]) -&gt; System.Object";
        
        Object request = myConnector.getRequest("/home/test/Documents/MonoEmbedded/Test.SampleComponent.dll", methodName, "Test.SampleComponent.Sample, Test.SampleComponent", methodArgs, null);
        Object response = myConnector.getDotNetBridge().processRequest(request);
        
        
        
		byte[] result = (byte[])myConnector.toPayload(response, null);
		String returnValue = new String(result);
		Assert.assertEquals(input + " Updated from .NET!", returnValue);
	}*/
	@Test
	public void testGetMetadata() throws Exception
	{
		DotNetConnector myConnector = new DotNetConnector();
		
		ResourceStrategy strategy = new ResourceStrategy();
		strategy.setPath("MuleTest.dll");
		myConnector.setConnectionStrategy(strategy);
		myConnector.setOnlyDeclared(true);
		
		DotNetMetadataCategory myCategory = new DotNetMetadataCategory();
		myCategory.setConnector(myConnector);
		
		List<ComposedMetaDataKey> keys = myCategory.getMetaDataKeys();
		
		int size = keys.size();
		
		Assert.assertTrue(size > 0);
	}
	
	@Test
	public void testGetMetadataFromResourceFolder() throws Exception
	{
		DotNetConnector myConnector = new DotNetConnector();
		
		ResourceStrategy strategy = new ResourceStrategy();
		strategy.setResourceFolder("Test.SampleComponent");
		strategy.setPath("Test.SampleComponent.dll");
		myConnector.setConnectionStrategy(strategy);
		myConnector.setOnlyDeclared(true);
		
		DotNetMetadataCategory myCategory = new DotNetMetadataCategory();
		myCategory.setConnector(myConnector);
		
		List<ComposedMetaDataKey> keys = myCategory.getMetaDataKeys();
		
		int size = keys.size();
		
		Assert.assertTrue(size > 0);
	}
	
	private ComposedMetaDataKey getKey(String methodName)
	{
		ComposedMetaDataKeyBuilder builder = ComposedMetaDataKeyBuilder.getInstance();
		
		ComposedMetaDataKeyBuilder.CombinationBuilder classBuilder = builder.newKeyCombination();
		
		classBuilder.newLevel().addId("dummy", "dummy").endLevel();
		
		classBuilder.newLevel()
		    .addId("Test.SampleComponent.Sample, Test.SampleComponent, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null", "Test.SampleComponent.Sample")
		    .endLevel();
		    		
		ComposedMetaDataKeyBuilder.LevelBuilder methodLevelBuilder = classBuilder.newLevel();

		methodLevelBuilder.addId(methodName, methodName);
		methodLevelBuilder.endLevel();
		classBuilder.endKeyCombination();
		return builder.build().get(0);
	}
	
	@Test
	public void testGetMetadataDescription() throws Exception
	{
		DotNetConnector myConnector = new DotNetConnector();

		ResourceStrategy strategy = new ResourceStrategy();
		strategy.setPath("Test.SampleComponent.dll");
		myConnector.setConnectionStrategy(strategy);
		
		DotNetMetadataCategory myCategory = new DotNetMetadataCategory();
		myCategory.setConnector(myConnector);

		ComposedMetaDataKey key = getKey("ExecuteComplexMix(Test.SampleComponent.Person person, System.Int32 id, System.Boolean increment) -&gt; System.Object");
		
		MetaData metadata = myCategory.describeMetadata(key);
		
		Assert.assertNotNull(metadata);
		DefaultDefinedMapMetaDataModel model = (DefaultDefinedMapMetaDataModel)metadata.getPayload();

		Assert.assertNotNull(model);
		
		Assert.assertEquals("ExecuteComplexMix", model.getName());
		
		Assert.assertEquals(3, model.getFields().size());
		
		Assert.assertEquals("person", model.getFields().get(0).getName());
		Assert.assertEquals("id", model.getFields().get(1).getName());
		Assert.assertEquals("increment", model.getFields().get(2).getName());
	}
	
	@Test
	public void testGetMetadataDescriptionForOverloads() throws Exception
	{
		DotNetConnector myConnector = new DotNetConnector();

		ResourceStrategy strategy = new ResourceStrategy();
		strategy.setPath("Test.SampleComponent.dll");
		myConnector.setConnectionStrategy(strategy);
		
		DotNetMetadataCategory myCategory = new DotNetMetadataCategory();
		myCategory.setConnector(myConnector);
		
		ComposedMetaDataKey key = getKey("Overload(System.Int32 a) -&gt; System.String");
		
		MetaData metadata = myCategory.describeMetadata(key);
		
		Assert.assertNotNull(metadata);
		DefaultDefinedMapMetaDataModel model = (DefaultDefinedMapMetaDataModel)metadata.getPayload();

		Assert.assertNotNull(model);
		
		Assert.assertEquals("Overload", model.getName());
		
		Assert.assertEquals(1, model.getFields().size());
		
		Assert.assertEquals("a", model.getFields().get(0).getName());
	}
	
	@Test
	public void testGetMetadataDescriptionForOverloads2Parameters() throws Exception
	{
		DotNetConnector myConnector = new DotNetConnector();

		ResourceStrategy strategy = new ResourceStrategy();
		strategy.setPath("Test.SampleComponent.dll");
		myConnector.setConnectionStrategy(strategy);
		
		DotNetMetadataCategory myCategory = new DotNetMetadataCategory();
		myCategory.setConnector(myConnector);
		
		ComposedMetaDataKey key = getKey("Overload(System.String a, System.Int32 b) -&gt; System.String");
		
		MetaData metadata = myCategory.describeMetadata(key);
		
		Assert.assertNotNull(metadata);
		DefaultDefinedMapMetaDataModel model = (DefaultDefinedMapMetaDataModel)metadata.getPayload();

		Assert.assertNotNull(model);
		
		Assert.assertEquals("Overload", model.getName());
		
		Assert.assertEquals(2, model.getFields().size());
		Assert.assertEquals("a", model.getFields().get(0).getName());
		Assert.assertEquals("b", model.getFields().get(1).getName());
	}
	
	@Test
	public void testGetMetadataDescriptionForGenericParameters() throws Exception
	{
		DotNetConnector myConnector = new DotNetConnector();

		ResourceStrategy strategy = new ResourceStrategy();
		strategy.setPath("Test.SampleComponent.dll");
		myConnector.setConnectionStrategy(strategy);
		
		DotNetMetadataCategory myCategory = new DotNetMetadataCategory();
		myCategory.setConnector(myConnector);
		
		ComposedMetaDataKey key = getKey("ParamsDictionary(System.String guid, System.Collections.Generic.Dictionary`2[System.String,System.String] runparams) -&gt; System.Object");
		
		MetaData metadata = myCategory.describeMetadata(key);
		
		Assert.assertNotNull(metadata);
		DefaultDefinedMapMetaDataModel model = (DefaultDefinedMapMetaDataModel)metadata.getPayload();

		Assert.assertNotNull(model);
		
		Assert.assertEquals("ParamsDictionary", model.getName());
		
		Assert.assertEquals(2, model.getFields().size());
		Assert.assertEquals("guid", model.getFields().get(0).getName());
		Assert.assertEquals("runparams", model.getFields().get(1).getName());
	}
	
	@Test
	public void testGetMetadataDescriptionForOverloadsParameterless() throws Exception
	{
		DotNetConnector myConnector = new DotNetConnector();

		ResourceStrategy strategy = new ResourceStrategy();
		strategy.setPath("Test.SampleComponent.dll");
		myConnector.setConnectionStrategy(strategy);
		
		DotNetMetadataCategory myCategory = new DotNetMetadataCategory();
		myCategory.setConnector(myConnector);

		ComposedMetaDataKey key = getKey("Overload() -&gt; System.String");
		
		MetaData metadata = myCategory.describeMetadata(key);
		
		Assert.assertNotNull(metadata);
		DefaultSimpleMetaDataModel model = (DefaultSimpleMetaDataModel)metadata.getPayload();

		Assert.assertNotNull(model);
	}
	
	@Test
	public void testGetOutputMetadataWithPrimitive() throws Exception
	{
		DotNetConnector myConnector = new DotNetConnector();

		ResourceStrategy strategy = new ResourceStrategy();
		strategy.setPath("Test.SampleComponent.dll");
		myConnector.setConnectionStrategy(strategy);
		
		DotNetMetadataCategory myCategory = new DotNetMetadataCategory();
		myCategory.setConnector(myConnector);

		ComposedMetaDataKey key = getKey("ConvertSpeedToKilometers(System.Collections.Generic.List`1[Test.SampleComponent.Car] cars) -&gt; System.Collections.Generic.List`1[Test.SampleComponent.Car]");
		
		MetaData metadata = myCategory.describeOutputMetadata(key);
		
		Assert.assertNotNull(metadata);
		DefaultDefinedMapMetaDataModel model = (DefaultDefinedMapMetaDataModel)metadata.getPayload();

		Assert.assertNotNull(model);
	}
}
