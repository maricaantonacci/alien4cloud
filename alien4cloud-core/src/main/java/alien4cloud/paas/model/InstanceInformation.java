package alien4cloud.paas.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnore;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InstanceInformation {

    /**
     * The textual representation of the state of the instance.
     */
	protected String state;

    /**
     * The effective representation of the state of the instance (SUCCESS, PROCESSING, FAILURE).
     */
    protected InstanceStatus instanceStatus;

    /** Values of attributes for this instance. */
    protected Map<String, String> attributes;
    /** Additional properties specific from the container. */
    protected Map<String, String> runtimeProperties;

    /** Available operations outputs for this node instance */
    /** do not serialize */
    @JsonIgnore
    protected Map<String, String> operationsOutputs;
    
    /**
     * The results returned from an Orchestrator grouped by the ID
     * of the output
     */
    protected Map<String, String> outputsResults;
    
}
