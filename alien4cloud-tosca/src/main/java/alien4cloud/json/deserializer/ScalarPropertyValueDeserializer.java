package alien4cloud.json.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ScalarPropertyValueDeserializer<T>  extends StdDeserializer<String>{

  protected ScalarPropertyValueDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

    ObjectMapper mapper = (ObjectMapper) p.getCodec();
    ObjectNode root = mapper.readTree(p);
    return root.toString();
  }

}
