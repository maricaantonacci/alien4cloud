package alien4cloud.tosca.serializer;

import static alien4cloud.utils.AlienUtils.safe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.alien4cloud.tosca.model.definitions.AbstractArtifact;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ConcatPropertyValue;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.definitions.OutputDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.AbstractPropertyConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterOrEqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.GreaterThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.InRangeConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessOrEqualConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.LessThanConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MaxLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.MinLengthConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.PatternConstraint;
import org.alien4cloud.tosca.model.definitions.constraints.ValidValuesConstraint;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.ServiceNodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.NodeWorkflowStep;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.CallOperationWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.DelegateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.InlineWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

/**
 * Tools for serializing in YAML/TOSCA. ALl methods should be static but did not
 * found how to use statics from velocity. Modified with info from
 * https://github.com/ConceptReplyIT/alien4cloud/commit/44a8c68293b2e6ad076e798690bfbd3fcdf7db24
 */
public class ToscaSerializerUtils {

  public static String testStatic() {
    return "testStatic";
  }

  public String testNonStatic() {
    return "testNonStatic";
  }

  /**
   * Method from
   * https://github.com/ConceptReplyIT/alien4cloud/commit/44a8c68293b2e6ad076e798690bfbd3fcdf7db24
   */
  public boolean isAbstractPropertyValueNotNullAndPrintable(AbstractPropertyValue value) {
    if (value == null) {
      return false;
      // IPrintable has disappeared
//        } else if (!value.isPrintable()) {
//            return false;
    } else if (value instanceof ScalarPropertyValue) {
      return ((ScalarPropertyValue) value).getValue() != null;
    } else {
      return true;
    }
  }

  public boolean collectionIsNotEmpty(Collection<?> c) {
    return c != null && !c.isEmpty();
  }

  public boolean mapIsNotEmpty(Map<?, ?> m) {
    return m != null && !m.isEmpty();
  }

  /**
   * Render a description. If the string contain CRLF, then render a multiline
   * literal preserving indentation.
   */
  public String renderDescription(String description, String identation) throws IOException {
    if (description != null && description.contains("\n")) {
      BufferedReader br = new BufferedReader(new StringReader(description));
      StringWriter sw = new StringWriter();
      sw.write("|");
      sw.write("\n");
      String line = br.readLine();
      boolean isFirst = true;
      while (line != null) {
        if (isFirst) {
          isFirst = false;
        } else {
          sw.write("\n");
        }
        sw.write(identation);
        sw.write(line);
        line = br.readLine();
      }
      return sw.toString();
    } else {
      return "\"" + ToscaPropertySerializerUtils.escapeDoubleQuote(description) + "\"";
    }
  }

  /**
   * Check if the map is not null, not empty and contains at least one not null
   * value. This function is recursive:
   * <ul>
   * <li>if a map entry is a also a map, then we'll look for non null values in it
   * (recursively).
   * <li>if a map entry is a collection, then will return true if the collection
   * is not empty.
   * <li>if a map entry is a ScalarPropertyValue, then will return true if the
   * value is not null.
   * </ul>
   */
  public boolean mapIsNotEmptyAndContainsNotnullValues(Map<?, ?> m) {
    if (mapIsNotEmpty(m)) {
      for (Object o : m.values()) {
        if (o != null) {
          if (o instanceof Map<?, ?>) {
            if (mapIsNotEmptyAndContainsNotnullValues((Map<?, ?>) o)) {
              return true;
            }
          } else if (o instanceof Collection<?>) {
            if (!((Collection<?>) o).isEmpty()) {
              return true;
            }
//                    } else if (o instanceof ScalarPropertyValue) {
//                        if (((ScalarPropertyValue) o).getValue() != null) {
//                            return true;
//                        }
          } else if (o instanceof AbstractPropertyValue) {
            if (isAbstractPropertyValueNotNullAndPrintable((AbstractPropertyValue) o)) {
              return true;
            }
          } else {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Method from
   * https://github.com/ConceptReplyIT/alien4cloud/commit/44a8c68293b2e6ad076e798690bfbd3fcdf7db24
   */
  public static boolean isScalarPropertyValue(Object o) {
    return o instanceof ScalarPropertyValue;
  }

  /**
   * Method from
   * https://github.com/ConceptReplyIT/alien4cloud/commit/44a8c68293b2e6ad076e798690bfbd3fcdf7db24
   */
  public static boolean isFunctionPropertyValue(Object o) {
    return o instanceof FunctionPropertyValue;
  }

  /**
   * Method from
   * https://github.com/ConceptReplyIT/alien4cloud/commit/44a8c68293b2e6ad076e798690bfbd3fcdf7db24
   */
  public static boolean isConcatPropertyValue(Object o) {
    return o instanceof ConcatPropertyValue;
  }

  public String getCsvToString(Collection<?> list) {
    return getCsvToString(list, false);
  }

  /**
   * Method from
   * https://github.com/ConceptReplyIT/alien4cloud/commit/44a8c68293b2e6ad076e798690bfbd3fcdf7db24
   */
  public String getCsvToString(Collection<?> list, boolean renderScalar) {
    StringBuilder sb = new StringBuilder();
    boolean isFirst = true;
    if (list != null) {
      for (Object o : list) {
        if (isFirst) {
          isFirst = false;
        } else {
          sb.append(", ");
        }
        // Replace the scalar checking
//                if (renderScalar) {
//                    sb.append(ToscaPropertySerializerUtils.renderScalar(o.toString()));
//                } else {
//                    sb.append(o.toString());
//                }
        if (isFunctionPropertyValue(o) || isConcatPropertyValue(o)) {
          sb.append(renderFunctionAndConcat((AbstractPropertyValue) o));
        } else if (isScalarPropertyValue(o)) {
          String stringValue = ((ScalarPropertyValue) o).getValue();
          if (renderScalar) {
            sb.append(ToscaPropertySerializerUtils.renderScalar(stringValue));
          } else {
            sb.append(stringValue);
          }
        } else {
          if (renderScalar) {
            sb.append(ToscaPropertySerializerUtils.renderScalar(o.toString()));
          } else {
            sb.append(o.toString());
          }
        }
      }
    }
    return sb.toString();
  }

  public boolean hasCapabilitiesContainingNotNullProperties(NodeTemplate nodeTemplate) {
    Map<String, Capability> capabilities = nodeTemplate.getCapabilities();
    if (capabilities == null || capabilities.isEmpty()) {
      return false;
    }
    for (Capability capability : capabilities.values()) {
      if (capability == null) {
        continue;
      }
      if (mapIsNotEmptyAndContainsNotnullValues(capability.getProperties())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Method from
   * https://github.com/ConceptReplyIT/alien4cloud/commit/44a8c68293b2e6ad076e798690bfbd3fcdf7db24
   */
  public static String renderFunctionAndConcat(Object apv) {
    StringBuilder result = new StringBuilder();
    if (apv instanceof Map) {
      result.append(" { ");
      Map<String, Object> node = ((Map<String, Object>) apv);
      result.append(node.get("function").toString()).append(": ")
          .append(renderFunctionAndConcat(node.get("parameters"))).append(" } ");
    } else if (apv instanceof List) {
      List<Object> node = (List<Object>) apv;

      if (node.size() > 1) {
        result.append(" [ ");
        for (Object el : node)
          result.append(renderFunctionAndConcat(el)).append(", ");
        result.delete(result.length() - 2, result.length());
        result.append(" ] ");
      } else if (node.size() == 1) {
        result.append(renderFunctionAndConcat(node.get(0)));
      }
    } else {
      String value = apv.toString();
      boolean hasNonAlpha = value.matches("^.*[^a-zA-Z0-9 ].*$");
      if (hasNonAlpha)
        result.append("'").append(value).append("'");
      else
        result.append(value);
    }
    return result.toString();

    /*
     * StringBuilder builder = new StringBuilder(); String functionName; List<?>
     * parameters = new ArrayList<>(); if (isFunctionPropertyValue(apv)) {
     * FunctionPropertyValue fpv = (FunctionPropertyValue) apv; functionName =
     * fpv.getFunction(); parameters = fpv.getParameters(); } else if
     * (isConcatPropertyValue(apv)) { ConcatPropertyValue cpv =
     * (ConcatPropertyValue) apv; functionName = cpv.getFunction_concat();
     * parameters = cpv.getParameters(); } else { return builder.toString(); }
     * builder.append(functionName).append(": [ ").append(getCsvToString(parameters,
     * true)).append(" ]"); return builder.toString();
     */

  }
  
  /**
   * Check if the other output fields in a topology have an ID similar to the one in overall outputs.
   * Due to the fact that the name of the other outputs is created dynamically during runtime in the TOSCA doc, 
   * we have to use the same algorithms to generate the name needed to check for the existence of 
   * duplicate outputs
   * 
   * @param topology The topology we want to check the outputs for
   * @param outputDefinition The overall output that we try to check if it is a duplicate 
   * @return
   */
  public static boolean isOutputUniqueByName(Topology topo, OutputDefinition<?> outputDefinition) {
    final String name = outputDefinition.getName().toLowerCase();
    if (topo.getOutputAttributes() != null) {
      for (Map.Entry<String, Set<String>> outputEntry: topo.getOutputAttributes().entrySet()) {
        for (String outputParam: outputEntry.getValue())
          if (name.compareToIgnoreCase(outputEntry.getKey() + "_" + outputParam) == 0)
            return true;
      }
    }
    if (topo.getOutputCapabilityProperties() != null) {
      for (Map.Entry<String, Map<String, Set<String>>> nodeTemplate: topo.getOutputCapabilityProperties().entrySet()) {
        for (Map.Entry<String, Set<String>> capability: nodeTemplate.getValue().entrySet()) {
          for (String outputProperty: capability.getValue()) {
            if (name.compareToIgnoreCase(nodeTemplate.getKey() + "_" + capability.getKey() + "_" + outputProperty) == 0)
              return true;
          }
        }
      }
    }
    if (topo.getOutputProperties() != null) { 
      for (Map.Entry<String, Set<String>> nodeTemplate: topo.getOutputAttributes().entrySet()) {
        for (String nodeTemplateAttribute: nodeTemplate.getValue())
          if (name.compareToIgnoreCase(nodeTemplate.getKey() + "_" + nodeTemplateAttribute) == 0)
            return true;
      }
    }
    return false;
  }

  public boolean doesInterfacesContainsImplementedOperation(Map<String, Interface> interfaces) {
    if (interfaces == null || interfaces.isEmpty()) {
      return false;
    }
    for (Interface interfaze : interfaces.values()) {
      if (doesInterfaceContainsImplementedOperation(interfaze)) {
        return true;
      }
    }
    return false;
  }

  public boolean doesInterfaceContainsImplementedOperation(Interface interfaze) {
    if (interfaze == null) {
      return false;
    }
    Map<String, Operation> operations = interfaze.getOperations();
    if (operations == null || operations.isEmpty()) {
      return false;
    }
    for (Operation operation : operations.values()) {
      if (isOperationImplemented(operation)) {
        return true;
      }
    }
    return false;
  }

  public boolean isOperationImplemented(Operation operation) {
    if (operation == null) {
      return false;
    }
    if (operation.getImplementationArtifact() != null) {
      return true;
    }
    return false;
  }

  public String renderConstraint(AbstractPropertyConstraint c) {
    StringBuilder builder = new StringBuilder();
    if (c instanceof GreaterOrEqualConstraint) {
      builder.append("greater_or_equal: ");
      builder.append(ToscaPropertySerializerUtils.renderScalar(((GreaterOrEqualConstraint) c).getGreaterOrEqual()));
    } else if (c instanceof GreaterThanConstraint) {
      builder.append("greater_than: ");
      builder.append(ToscaPropertySerializerUtils.renderScalar(((GreaterThanConstraint) c).getGreaterThan()));
    } else if (c instanceof LessOrEqualConstraint) {
      builder.append("less_or_equal: ");
      builder.append(ToscaPropertySerializerUtils.renderScalar(((LessOrEqualConstraint) c).getLessOrEqual()));
    } else if (c instanceof LessThanConstraint) {
      builder.append("less_than: ");
      builder.append(ToscaPropertySerializerUtils.renderScalar(((LessThanConstraint) c).getLessThan()));
    } else if (c instanceof LengthConstraint) {
      builder.append("length: ");
      builder.append(((LengthConstraint) c).getLength());
    } else if (c instanceof MaxLengthConstraint) {
      builder.append("max_length: ");
      builder.append(((MaxLengthConstraint) c).getMaxLength());
    } else if (c instanceof MinLengthConstraint) {
      builder.append("min_length: ");
      builder.append(((MinLengthConstraint) c).getMinLength());
    } else if (c instanceof PatternConstraint) {
      builder.append("pattern: ");
      builder.append(ToscaPropertySerializerUtils.renderScalar(((PatternConstraint) c).getPattern()));
    } else if (c instanceof EqualConstraint) {
      builder.append("equal: ");
      builder.append(ToscaPropertySerializerUtils.renderScalar(((EqualConstraint) c).getEqual()));
    } else if (c instanceof InRangeConstraint) {
      builder.append("in_range: ");
      builder.append("[");
      builder.append(getCsvToString(((InRangeConstraint) c).getInRange(), true));
      builder.append("]");
    } else if (c instanceof ValidValuesConstraint) {
      builder.append("valid_values: ");
      builder.append("[");
      builder.append(getCsvToString(((ValidValuesConstraint) c).getValidValues(), true));
      builder.append("]");
    }
    return builder.toString();
  }

  public boolean isNodeActivityStep(WorkflowStep abstractStep) {
    return abstractStep instanceof NodeWorkflowStep;
  }

  public String getActivityLabel(AbstractWorkflowActivity activity) {
    if (activity instanceof CallOperationWorkflowActivity) {
      return "call_operation";
    } else if (activity instanceof SetStateWorkflowActivity) {
      return "set_state";
    } else if (activity instanceof DelegateWorkflowActivity) {
      return "delegate";
    } else if (activity instanceof InlineWorkflowActivity) {
      return "inline";
    } else {
      return activity.getClass().getSimpleName();
    }
  }

  public String getInlineActivityArg(AbstractWorkflowActivity activity) {
    if (activity instanceof CallOperationWorkflowActivity) {
      CallOperationWorkflowActivity callActivity = (CallOperationWorkflowActivity) activity;
      return callActivity.getInterfaceName() + "." + callActivity.getOperationName();
    } else if (activity instanceof SetStateWorkflowActivity) {
      SetStateWorkflowActivity stateActivity = (SetStateWorkflowActivity) activity;
      return stateActivity.getStateName();
    } else if (activity instanceof DelegateWorkflowActivity) {
      DelegateWorkflowActivity delegateWorkflowActivity = (DelegateWorkflowActivity) activity;
      return delegateWorkflowActivity.getDelegate();
    } else if (activity instanceof InlineWorkflowActivity) {
      return ((InlineWorkflowActivity) activity).getInline();
    } else {
      return "void";
    }
  }

  public static boolean hasRepositories(String topologyArchiveName, String topologyArchiveVersion, Topology topology) {
    // we don't support node types in Editor context, just check the node templates
    for (NodeTemplate node : safe(topology.getNodeTemplates()).values()) {
      for (DeploymentArtifact artifact : safe(node.getArtifacts()).values()) {
        // Only consider artifact of the topology
        if (isInternalRepoArtifact(artifact, topologyArchiveName, topologyArchiveVersion)) {
          return true;
        }
      }
      for (Interface anInterface : safe(node.getInterfaces()).values()) {
        for (Operation operation : safe(anInterface.getOperations()).values()) {
          if (operation.getImplementationArtifact() != null && isInternalRepoArtifact(
              operation.getImplementationArtifact(), topologyArchiveName, topologyArchiveVersion)) {
            return true;
          }
        }
      }
    }

    return MapUtils.isNotEmpty(topology.getInputArtifacts()) && topology.getInputArtifacts().values().stream()
        .anyMatch(deploymentArtifact -> StringUtils.isNotBlank(deploymentArtifact.getRepositoryName()));
  }

  private static boolean isInternalRepoArtifact(AbstractArtifact artifact, String topologyArchiveName,
      String topologyArchiveVersion) {
    return (topologyArchiveName.equals(artifact.getArchiveName())
        && topologyArchiveVersion.equals(artifact.getArchiveVersion()))
        && StringUtils.isNotBlank(artifact.getArtifactRepository())
        && StringUtils.isNotBlank(artifact.getRepositoryName());
  }

  public static String formatRepositories(String topologyArchiveName, String topologyArchiveVersion,
      Topology topology) {
    StringBuilder buffer = new StringBuilder();
    Set<String> repositoriesName = Sets.newHashSet();
    for (NodeTemplate node : safe(topology.getNodeTemplates()).values()) {
      for (DeploymentArtifact artifact : safe(node.getArtifacts()).values()) {
        // Only generate repositories for the current topology
        if (isInternalRepoArtifact(artifact, topologyArchiveName, topologyArchiveVersion)) {
          buffer.append("  ").append(artifact.getRepositoryName()).append(":");
          buffer.append("\n").append(formatRepository(artifact, 2)).append("\n");
        }
      }
      for (Interface anInterface : safe(node.getInterfaces()).values()) {
        for (Operation operation : safe(anInterface.getOperations()).values()) {
          if (operation.getImplementationArtifact() != null && isInternalRepoArtifact(
              operation.getImplementationArtifact(), topologyArchiveName, topologyArchiveVersion)) {
            buffer.append("  ").append(operation.getImplementationArtifact().getRepositoryName()).append(":");
            buffer.append("\n").append(formatRepository(operation.getImplementationArtifact(), 2)).append("\n");
          }
        }
      }
    }
    if (MapUtils.isNotEmpty(topology.getInputArtifacts())) {
      topology.getInputArtifacts().values().forEach(inputArtifact -> {
        if (StringUtils.isNotBlank(inputArtifact.getRepositoryURL())
            && repositoriesName.add(inputArtifact.getRepositoryName())) {
          buffer.append("  ").append(inputArtifact.getRepositoryName()).append(":");
          buffer.append("\n").append(formatRepository(inputArtifact, 2)).append("\n");
        }
      });
    }
    buffer.setLength(buffer.length() - 1);
    return buffer.toString();
  }

  public static String formatRepository(AbstractArtifact value, int indent) {
    StringBuilder buffer = new StringBuilder();
    String spaces = ToscaPropertySerializerUtils.indent(indent);
    if (StringUtils.isNotBlank(value.getRepositoryURL())) {
      buffer.append(spaces).append("url: ").append(value.getRepositoryURL());
    }
    buffer.append("\n").append(spaces).append("type: ").append(value.getArtifactRepository());
    if (value.getRepositoryCredential() != null && value.getRepositoryCredential().containsKey("token")) {
      buffer.append("\n").append(spaces).append("credential:");
      spaces += "  ";
      buffer.append("\n").append(spaces).append("token: ").append(value.getRepositoryCredential().get("token"));
      if (value.getRepositoryCredential().containsKey("user")) {
        buffer.append("\n").append(spaces).append("user: ").append(value.getRepositoryCredential().get("user"));
      }
    }
    return buffer.toString();
  }

  public static String formatArtifact(AbstractArtifact value, int indent) {
    String spaces = ToscaPropertySerializerUtils.indent(indent);
    StringBuilder buffer = new StringBuilder();
    if (StringUtils.isNotBlank(value.getArtifactRef())) {
      buffer//.append(spaces).append("file: ")
        .append("'").append(value.getArtifactRef()).append("'").append("\n");
    }
//    if (StringUtils.isNotBlank(value.getArtifactType())) {
//      buffer.append(spaces).append("type: ").append(value.getArtifactType()).append("\n");
//    }
    else if (StringUtils.isNotBlank(value.getRepositoryName())) {
      buffer//.append(spaces).append("repository: ")
        .append("'").append(value.getRepositoryName()).append("'").append("\n");
    }
    if (buffer.length() > 1) {
      buffer.setLength(buffer.length() - 1);
    }
    return buffer.toString();
  }
  
  public static String formatArtifactNodeTemplateArtifact(AbstractArtifact value, int indent) {
    String spaces = ToscaPropertySerializerUtils.indent(indent);
    StringBuilder buffer = new StringBuilder();
    if (StringUtils.isNotBlank(value.getArtifactRef())) {
        buffer.append(spaces).append("file: ").append(value.getArtifactRef()).append("\n");
    }
    if (StringUtils.isNotBlank(value.getArtifactType())) {
        buffer.append(spaces).append("type: ").append(value.getArtifactType()).append("\n");
    }
    if (StringUtils.isNotBlank(value.getRepositoryName())) {
        buffer.append(spaces).append("repository: ").append(value.getRepositoryName()).append("\n");
    }
    if (buffer.length() > 1) {
        buffer.setLength(buffer.length() - 1);
    }
    return buffer.toString();
}

  public static boolean canUseShortNotationForImplementationArtifact(Operation operation) {
    return MapUtils.isEmpty(operation.getInputParameters())
        && StringUtils.isEmpty(operation.getImplementationArtifact().getRepositoryName());
  }

  private static final Pattern GET_INPUT_ARTIFACT_PATTERN = Pattern.compile("\\{ *get_input_artifact: +[^}]+}");

  public Map<String, DeploymentArtifact> getTopologyArtifacts(String topologyArchiveName, String topologyArchiveVersion,
      Map<String, DeploymentArtifact> artifacts) {
    if (artifacts == null) {
      return Collections.emptyMap();
    }
    // Only generate artifacts that are really stored inside the topology
    return artifacts.entrySet().stream()
        .filter(artifact -> (topologyArchiveName.equals(artifact.getValue().getArchiveName())
            && topologyArchiveVersion.equals(artifact.getValue().getArchiveVersion()))
            || "alien_topology".equals(artifact.getValue().getArtifactRepository())
            || (artifact.getValue().getArtifactRef() != null
                && GET_INPUT_ARTIFACT_PATTERN.matcher(artifact.getValue().getArtifactRef()).matches()))
        .collect(Collectors.toMap(Map.Entry::getKey, (Map.Entry::getValue)));
  }

  public Map<String, AbstractPropertyValue> getServiceAttributes(NodeTemplate nodeTemplate) {
    if (nodeTemplate instanceof ServiceNodeTemplate) {
      ServiceNodeTemplate serviceNodeTemplate = (ServiceNodeTemplate) nodeTemplate;
      if (serviceNodeTemplate.getAttributeValues() != null) {
        return serviceNodeTemplate.getAttributeValues().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ScalarPropertyValue(entry.getValue())));
      } else {
        return Collections.emptyMap();
      }
    } else {
      return Collections.emptyMap();
    }
  }

  public Map<String, DeploymentArtifact> getServiceRelationshipArtifacts(NodeTemplate source, NodeTemplate target,
      RelationshipTemplate relationshipTemplate) {
    if (source instanceof ServiceNodeTemplate || target instanceof ServiceNodeTemplate) {
      return relationshipTemplate.getArtifacts();
    } else {
      return Collections.emptyMap();
    }
  }

  public static boolean isNull(Object o) {
    return o == null;
  }
}
