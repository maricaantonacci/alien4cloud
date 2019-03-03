package alien4cloud.paas.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstanceInformationEnhanced extends InstanceInformation {
//
//	public InstanceInformationEnhanced(String state, InstanceStatus instanceStatus, Map<String, String> attributes,
//			Map<String, String> runtimeProperties, Map<String, String> operationsOutputs, Map<String, String> outputsResults) {
//		super(state, instanceStatus, attributes, runtimeProperties, operationsOutputs);
//		this.outputsResults = outputsResults;
//	}

    /**
     * The results returned from an Orchestrator grouped by the ID
     * of the output
     */
    protected Map<String, String> outputsResults;
}
