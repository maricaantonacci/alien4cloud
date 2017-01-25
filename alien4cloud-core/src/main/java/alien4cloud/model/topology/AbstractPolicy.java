package alien4cloud.model.topology;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.model.components.AbstractPropertyValue;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractPolicy {

    private String name;
    
    private String description;

    private Map<String, AbstractPropertyValue> properties = Maps.newHashMap();
    
    private List<String> targets = Lists.newArrayList();

    public abstract String getType();

    // needed for JSON deserialization ?
    public abstract void setType(String type);

}
