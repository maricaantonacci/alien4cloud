package alien4cloud.json.deserializer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ConcatPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import alien4cloud.rest.utils.RestMapper;
import alien4cloud.utils.jackson.ConditionalAttributes;

/**
 * Custom deserializer to handle multiple IOperationParameter types.
 */
public class PropertyValueDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<AbstractPropertyValue> {
  public static final Integer FUNCTION_PROPERTY_VALUE = 2;
  public static final Integer NON_FUNCTION_PROPERTY_VALUE = 4;
  
  public PropertyValueDeserializer() {
        super(AbstractPropertyValue.class);
        addToRegistry("function_concat", ConcatPropertyValue.class);
        addToRegistry("function", FunctionPropertyValue.class);
        // let's handle null with a scalar deserializer.
        addToRegistry("value", JsonNodeType.NULL.toString(), ScalarPropertyValue.class);
        addToRegistry("value", JsonNodeType.STRING.toString(), ScalarPropertyValue.class);
        addToRegistry("value", JsonNodeType.ARRAY.toString(), ListPropertyValue.class);
        addToRegistry("value", JsonNodeType.OBJECT.toString(), ComplexPropertyValue.class);
        setValueStringClass(ScalarPropertyValue.class);
    }

    @Override
    public AbstractPropertyValue getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        if (ctxt.getAttribute(ConditionalAttributes.REST) != null && RestMapper.PATCH.equals(RestMapper.REQUEST_OPERATION.get())) {
            try {
                AbstractPropertyValue instance = (AbstractPropertyValue) RestMapper.NULL_INSTANCES.get(ScalarPropertyValue.class);
                if (instance == null) {
                    instance = ScalarPropertyValue.class.getConstructor().newInstance();
                }
                RestMapper.NULL_INSTANCES.put(ScalarPropertyValue.class, instance);
                return instance;
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            }
        }
        return super.getNullValue(ctxt);
    }
    
    @Override
    public AbstractPropertyValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode root = (JsonNode) mapper.readTree(jp);      
        

        return doDeserialize(mapper, root);//mapper.treeToValue(root, parameterClass);
    }
    
    protected AbstractPropertyValue doDeserialize(ObjectMapper mapper, JsonNode root) throws JsonProcessingException {
      if (root.isArray()) {
        ArrayNode an = ((ArrayNode)root);
        List<Object> result = new ArrayList<>();
        for (JsonNode node: an) 
          result.add(doDeserialize(mapper, node));
        return new ListPropertyValue(result);
      } else if (root.isValueNode()) {
        return new ScalarPropertyValue(root.asText());
      } else {
        final Set<Integer> propertyValueType = new HashSet<>();
        root.fieldNames().forEachRemaining(member -> {
          if (member.compareTo("function") == 0)
            propertyValueType.add(FUNCTION_PROPERTY_VALUE);
          else if (member.compareTo("value") == 0) {
            propertyValueType.add(NON_FUNCTION_PROPERTY_VALUE);
          }
        });
        if (propertyValueType.contains(FUNCTION_PROPERTY_VALUE)) {
          List<Object> params = new ArrayList<>();
          FunctionPropertyValue result = new FunctionPropertyValue(root.get("function").asText(), params);
          ArrayNode an = ((ArrayNode)root.get("parameters"));
          for (JsonNode node: an) {
            if (node.isValueNode())
              params.add(node.asText());
            else
              params.add(doDeserialize(mapper, node));
          }
          return result;
        } else if (propertyValueType.contains(NON_FUNCTION_PROPERTY_VALUE)) {
          return doDeserialize(mapper, root.get("value"));          
        } else {
          Iterator<Map.Entry<String, JsonNode>> elementsIterator = root.fields();
          Map<String, Object> result = new HashMap<>();
          while (elementsIterator.hasNext()) {
              Map.Entry<String, JsonNode> element = elementsIterator.next();
              Object val = doDeserialize(mapper, element.getValue());
              result.put(element.getKey(), val);
          }
          return new ComplexPropertyValue(result);
        }
      }
    }
}