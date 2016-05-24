package org.mule.modules.dotnet.metadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.mule.api.annotations.MetaDataKeyRetriever;
import org.mule.api.annotations.MetaDataOutputRetriever;
import org.mule.api.annotations.MetaDataRetriever;
import org.mule.api.annotations.components.MetaDataCategory;
import org.mule.common.metadata.DefaultDefinedMapMetaDataModel;
import org.mule.common.metadata.DefaultMetaData;
import org.mule.common.metadata.DefaultMetaDataField;
import org.mule.common.metadata.DefaultSimpleMetaDataModel;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataField;
import org.mule.common.metadata.MetaDataKey;
import org.mule.common.metadata.MetaDataModel;
import org.mule.common.metadata.builder.DefaultMetaDataBuilder;
import org.mule.common.metadata.builder.DynamicObjectBuilder;
import org.mule.common.metadata.datatype.DataType;
import org.mule.devkit.api.metadata.ComposedMetaDataKey;
import org.mule.devkit.api.metadata.ComposedMetaDataKeyBuilder;
import org.mule.modules.dotnet.DotNetConnector;

import com.google.common.base.Strings;

@MetaDataCategory
public class DotNetMetadataCategory 
{
	@Inject
	private DotNetConnector connector;
	
	public DotNetMetadataCategory() {}
	
	private ObjectMapper jsonMapper = new ObjectMapper();
    
    @MetaDataKeyRetriever
    public List<ComposedMetaDataKey> getMetaDataKeys() throws Exception 
    {
        try 
        {
        	String jsonData = this.connector.getDotNetBridge().getTypeMetadata(this.connector.getConnectionStrategy().getAssemblyInfo(), this.connector.getOnlyDeclared(), this.connector.getIncludeAutoGenerated());
    		return getDotNetTypes(jsonData);
     	} 
        catch (Exception e) 
        {
        	log("error in getMetaDataKeys: " + e.getMessage() + e.getStackTrace());
            throw e;
        }
    }
    
    private List<ComposedMetaDataKey> getDotNetTypes(String data) throws Exception 
    {
    	String argumentFormat = "%s %s, ";
        ObjectMapper jsonMapper = new ObjectMapper();

        Set<TypeMetadata> result = jsonMapper.readValue(data, new TypeReference<Set<TypeMetadata>>(){});
        ComposedMetaDataKeyBuilder builder = ComposedMetaDataKeyBuilder.getInstance();
        
        for (TypeMetadata type : result)
        {
    		String typeFullName = type.getFullName();

    		ComposedMetaDataKeyBuilder.CombinationBuilder classBuilder = builder.newKeyCombination();
    		classBuilder.newLevel()
    					.addId(typeFullName, type.getName())
						.endLevel();
    		
    		boolean hasMethods = false;
    		ComposedMetaDataKeyBuilder.LevelBuilder methodLevelBuilder = null;
            
    		for(MethodMetadata method : type.getMethods())
            {
    			if(hasMethods == false)
    			{
    				hasMethods = true;
    				methodLevelBuilder = classBuilder.newLevel();
    			}
                StringBuilder sb = new StringBuilder();
                StringBuilder sbDisplay = new StringBuilder();
                sb.append("(");
                sbDisplay.append("(");
                
                for(Argument arg : method.getParameters())
                {
                    sb.append(String.format(argumentFormat, arg.getType(), arg.getName()));
                    sbDisplay.append(String.format(argumentFormat, arg.getDisplayType(), arg.getName()));
                }
                
                if(sb.length() > 1)
                {
					sb = sb.delete(sb.length()-2, sb.length());
                }
                
                if(sbDisplay.length() > 1)
                {
                	sbDisplay = sbDisplay.delete(sbDisplay.length()-2, sbDisplay.length());
                }
				
				sb.append(")");
				sbDisplay.append(")");
				
                String methodData = String.format("%s | %s%s -> %s", type.getFullName(), method.getName(), sb.toString(), method.getReturnValue().getType());

                String displayName = String.format("%s %s%s", method.getReturnValue().getDisplayType(), method.getName(), sbDisplay.toString());
                methodLevelBuilder.addId(methodData, displayName);
            }
    		
    		if(methodLevelBuilder != null)
    		{
    			methodLevelBuilder.endLevel();
    		}
        	
    		classBuilder.endKeyCombination();
        }
        
    	return builder.build();
    }
    
    private MethodMetadata GetMethod(ComposedMetaDataKey key) throws Exception
    {
    	try 
		{
	        List<String> ids = key.getSortedIds();
	        
			String typeName = ids.get(1).trim();
			
			typeName = typeName.split(",")[0];
			String methodToInvoke = ids.get(2).trim();
			
			String jsonData = this.connector.getDotNetBridge().getMethodMetadata(this.connector.getConnectionStrategy().getAssemblyInfo(), typeName, methodToInvoke);
			MethodMetadata result = jsonMapper.readValue(jsonData, new TypeReference<MethodMetadata>(){});
			
			return result;
		} 
		catch (Exception e) 
		{
			log("describeMetadata: " + e.getMessage());
		    throw e;
		}
    }

    @MetaDataRetriever
    public MetaData describeMetadata(ComposedMetaDataKey key) throws Exception 
    {
    	try 
		{
    		MethodMetadata requestedMethod = GetMethod(key);
			Map<String, MetaDataModel> map = new LinkedHashMap<String, MetaDataModel>();
			
			if(!requestedMethod.getParameters().isEmpty())
			{
		        for (Argument arg : requestedMethod.getParameters()) 
		        {
	        		if(!arg.getIsComplex())
	        		{
	        	 		map.put(arg.getName(), new DefaultSimpleMetaDataModel(getDataType(arg)));
	        	 	}
	        	 	else
	        	 	{
	        	 		List<MetaDataField> fields = getComplexFields(arg);
	        			
	        			DefaultDefinedMapMetaDataModel complexModel = new DefaultDefinedMapMetaDataModel(fields);
	        			
	        			map.put(arg.getName(), complexModel);
	        	 	}
		         }
		        
		        MetaDataModel model = new DefaultDefinedMapMetaDataModel(convertMapToList(map), requestedMethod.getName());
		        return new DefaultMetaData(model);
			}
			else
			{
				MetaDataModel model = new DefaultSimpleMetaDataModel(DataType.VOID);
		        return new DefaultMetaData(model);					
	        }
		} 
		catch (Exception e) 
		{
			log("describeMetadata: " + e.getMessage());
		    throw e;
		}
    }
    
    @MetaDataOutputRetriever
	public MetaData describeOutputMetadata(ComposedMetaDataKey key) throws Exception
	{
    	try 
		{
    		MethodMetadata requestedMethod = GetMethod(key);
	        
    		Argument returnType = requestedMethod.getReturnValue();

    		List<MetaDataField> fields = new ArrayList<MetaDataField>();
    		if(!returnType.getIsComplex())
			{
				fields.add(new DefaultMetaDataField(returnType.getName(), new DefaultSimpleMetaDataModel(getDataType(returnType)), MetaDataField.FieldAccessType.READ_WRITE));
			}
			else
			{
            	fields = getComplexFields(returnType);
			}
    		
            return new DefaultMetaData(new DefaultDefinedMapMetaDataModel(fields));
		} 
		catch (Exception e) 
		{
			log("describeMetadata: " + e.getMessage());
		    throw e;
		}
	}
    
    private static void log(String data)
    {
    	/*java.util.logging.Logger logger = java.util.logging.Logger.getLogger("MyLog");  
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
        } */
    }
    
    private List<MetaDataField> getComplexFields(Argument arg)
    {
 		List<MetaDataField> fields = new ArrayList<MetaDataField>();
 		
	 	// there's a circular reference to a parent, return empty
		if(arg.getFields() == null)
		{
			return fields;
		}
 		
		for (Argument argument : arg.getFields()) 
		{
			if(!argument.getIsComplex())
			{
				fields.add(new DefaultMetaDataField(argument.getName(), new DefaultSimpleMetaDataModel(getDataType(argument)), MetaDataField.FieldAccessType.READ_WRITE));
			}
			else
			{
				fields.add(new DefaultMetaDataField(argument.getName(), new DefaultDefinedMapMetaDataModel(getComplexFields(argument))));
			}	
		}
		
		return fields;
    }
    
    private static DataType getDataType(Argument arg)
	{
		switch(arg.getType())
		{
			case "System.String" : return DataType.STRING;
			case "System.Boolean" : return DataType.BOOLEAN;
			case "System.Char" : return DataType.STRING;
			case "System.SByte" : return DataType.BYTE;
			case "System.Int16" : return DataType.SHORT;
			case "System.Int32" : return DataType.INTEGER;
			case "System.Int64" : return DataType.LONG;
			case "System.Float" : return DataType.FLOAT;
			case "System.Double" : return DataType.DOUBLE;
			case "System.DateTime" : return DataType.DATE_TIME;
			case "System.Decimal" : return DataType.DECIMAL;
			case "System.Guid" : return DataType.STRING;
			default : return DataType.STRING;
		}
	}
    
    private static List<MetaDataField> convertMapToList(Map<String, ? extends MetaDataModel> metaDataModelMap) {
        List<MetaDataField> mappedFields = new ArrayList<MetaDataField>();
        for (Map.Entry<String, ? extends MetaDataModel> entry : metaDataModelMap.entrySet()) {
            mappedFields.add(new DefaultMetaDataField(entry.getKey(), entry.getValue()));
        }
        return mappedFields;
    }
   
    public void setConnector(DotNetConnector connector) 
    { 
    	this.connector = connector; 
    }
}