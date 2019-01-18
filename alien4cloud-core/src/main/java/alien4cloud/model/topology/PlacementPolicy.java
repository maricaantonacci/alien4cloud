package alien4cloud.model.topology;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlacementPolicy extends AbstractPolicy {
    public static final String PLACEMENT_POLICY = "tosca.policies.Placement";
    public static final String PLACEMENT_ID_PROPERTY = "sla_id";

    @Override
    public String getType() {
        return PLACEMENT_POLICY;
    }

    @Override
    public void setType(String type) {
        // for json serialization
    }
}