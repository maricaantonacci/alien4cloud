package alien4cloud.json.serializer;

import java.io.IOException;

import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ScalarPropertyValueSerializer extends StdSerializer<ScalarPropertyValue>{

  protected ScalarPropertyValueSerializer() {
    super(ScalarPropertyValue.class, false);
  }

  @Override
  public void serialize(ScalarPropertyValue value, 
      JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(value.getValue().toString());
  }

}
