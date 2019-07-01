package alien4cloud.tosca.serializer;

import org.apache.commons.collections4.MapUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Strings;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.ConcatPropertyValue;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IPrintable;
import alien4cloud.model.components.IValue;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.Operation;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.components.constraints.AbstractPropertyConstraint;
import alien4cloud.model.components.constraints.EqualConstraint;
import alien4cloud.model.components.constraints.GreaterOrEqualConstraint;
import alien4cloud.model.components.constraints.GreaterThanConstraint;
import alien4cloud.model.components.constraints.InRangeConstraint;
import alien4cloud.model.components.constraints.LengthConstraint;
import alien4cloud.model.components.constraints.LessOrEqualConstraint;
import alien4cloud.model.components.constraints.LessThanConstraint;
import alien4cloud.model.components.constraints.MaxLengthConstraint;
import alien4cloud.model.components.constraints.MinLengthConstraint;
import alien4cloud.model.components.constraints.PatternConstraint;
import alien4cloud.model.components.constraints.ValidValuesConstraint;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.paas.wf.AbstractActivity;
import alien4cloud.paas.wf.AbstractStep;
import alien4cloud.paas.wf.DelegateWorkflowActivity;
import alien4cloud.paas.wf.NodeActivityStep;
import alien4cloud.paas.wf.OperationCallActivity;
import alien4cloud.paas.wf.SetStateActivity;

/**
 * Tools for serializing in YAML/TOSCA. ALl methods should be static but did not found how to use statics from velocity.
 */
public class ToscaSerializerUtils {

    private static Pattern ESCAPE_PATTERN = Pattern.compile(".*[,:\\[\\]\\{\\}-].*");

    public boolean collectionIsNotEmpty(Collection<?> c) {
        return c != null && !c.isEmpty();
    }

    public boolean mapIsNotEmpty(Map<?, ?> m) {
        return m != null && !m.isEmpty();
    }

    /**
     * Render the scalar: when it contain '[' or ']' or '{' or '}' or ':' or '-' or ',', then quote the scalar.
     */
    public String renderScalar(String scalar) {
        if (scalar == null) {
            return null;
        } else if (ESCAPE_PATTERN.matcher(scalar).matches()) {
            return "\"" + escapeDoubleQuote(scalar) + "\"";
        } else if (scalar.startsWith(" ") || scalar.endsWith(" ")) {
            return "\"" + escapeDoubleQuote(scalar) + "\"";
        } else if (scalar.isEmpty()) {
            return "\"\"";
        } else {
            return scalar;
        }
    }

    public boolean isAbstractPropertyValueNotNullAndPrintable(AbstractPropertyValue value) {
        if (value == null) {
            return false;
        } else if (!value.isPrintable()) {
            return false;
        } else if (value instanceof ScalarPropertyValue) {
            return ((ScalarPropertyValue) value).getValue() != null;
        } else {
            return true;
        }
    }

    private static String escapeDoubleQuote(String scalar) {
        if (scalar != null && scalar.contains("\"")) {
            // escape double quote
            return scalar.replaceAll("\"", "\\\\\"");
        }
        return scalar;
    }

    /**
     * Render a description. If the string contain CRLF, then render a multiline literal preserving indentation.
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
            return description;
        }
    }

    /**
     * Check if the map is not null, not empty and contains at least one not null value.
     * This function is recursive:
     * <ul>
     * <li>if a map entry is a also a map, then we'll look for non null values in it (recursively).
     * <li>if a map entry is a collection, then will return true if the collection is not empty.
     * <li>if a map entry is a ScalarPropertyValue, then will return true if the value is not null.
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

    public boolean isScalarPropertyValue(Object o) {
        return o instanceof ScalarPropertyValue;
    }

    public boolean isFunctionPropertyValue(Object o) {
        return o instanceof FunctionPropertyValue;
    }

    public String getCsvToString(Collection<?> list) {
        return getCsvToString(list, false);
    }

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
                if (isFunctionPropertyValue(o) || isConcatPropertyValue(o)) {
                  sb.append(renderFunctionAndConcat((AbstractPropertyValue) o));
                } else if (isScalarPropertyValue(o)) {
                  String stringValue = ((ScalarPropertyValue) o).getValue();
                  if (renderScalar) {
                    sb.append(renderScalar(stringValue));
                  } else {
                    sb.append(stringValue);
                  }
                } else {
                  if (renderScalar) {
                      sb.append(renderScalar(o.toString()));
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

    public String renderConstraint(AbstractPropertyConstraint c) {
        StringBuilder builder = new StringBuilder();
        if (c instanceof GreaterOrEqualConstraint) {
            builder.append("greater_or_equal: ");
            builder.append(renderScalar(((GreaterOrEqualConstraint) c).getGreaterOrEqual()));
        } else if (c instanceof GreaterThanConstraint) {
            builder.append("greater_than: ");
            builder.append(renderScalar(((GreaterThanConstraint) c).getGreaterThan()));
        } else if (c instanceof LessOrEqualConstraint) {
            builder.append("less_or_equal: ");
            builder.append(renderScalar(((LessOrEqualConstraint) c).getLessOrEqual()));
        } else if (c instanceof LessThanConstraint) {
            builder.append("less_than: ");
            builder.append(renderScalar(((LessThanConstraint) c).getLessThan()));
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
            builder.append(renderScalar(((PatternConstraint) c).getPattern()));
        } else if (c instanceof EqualConstraint) {
            builder.append("equal: ");
            builder.append(renderScalar(((EqualConstraint) c).getEqual()));
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

    public boolean isNodeActivityStep(AbstractStep abstractStep) {
        return abstractStep instanceof NodeActivityStep;
    }

    public String getActivityLabel(AbstractActivity activity) {
        if (activity instanceof OperationCallActivity) {
            return "call_operation";
        } else if (activity instanceof SetStateActivity) {
            return "set_state";
        } else if (activity instanceof DelegateWorkflowActivity) {
            return "delegate";
        } else {
            return activity.getClass().getSimpleName();
        }
    }

    public boolean canRenderInlineActivityArgs(AbstractActivity activity) {
        // if return false, the renderer will call getActivityArgsMap, elsewhere getActivityArg
        return true;
    }

    public String getInlineActivityArg(AbstractActivity activity) {
        if (activity instanceof OperationCallActivity) {
            OperationCallActivity callActivity = (OperationCallActivity) activity;
            return callActivity.getInterfaceName() + "." + callActivity.getOperationName();
        } else if (activity instanceof SetStateActivity) {
            SetStateActivity stateActivity = (SetStateActivity) activity;
            return stateActivity.getStateName();
        } else if (activity instanceof DelegateWorkflowActivity) {
            DelegateWorkflowActivity delegateWorkflowActivity = (DelegateWorkflowActivity) activity;
            return delegateWorkflowActivity.getWorkflowName();
        } else {
            return "void";
        }
    }

    // sample map for complex activity that can not be rendered simply
    public Map<String, String> getActivityArgsMap(AbstractActivity activity) {
        Map<String, String> args = new HashMap<String, String>();
        args.put("arg1", "value1");
        args.put("arg2", "value2");
        return args;
    }

    public boolean isListPropertyValue(Object o) {
        return o instanceof ListPropertyValue;
    }

    public boolean isConcatPropertyValue(Object o) {
        return o instanceof ConcatPropertyValue;
    }

    public boolean isComplexPropertyValue(Object o) {
        return o instanceof ComplexPropertyValue;
    }

    public boolean hasArtifactsContainingNotNullValues(NodeTemplate nodeTemplate) {
      Map<String, DeploymentArtifact> artifacts = nodeTemplate.getArtifacts();
      if (artifacts == null || artifacts.isEmpty()) {
        return false;
      }
      for (DeploymentArtifact artifact : artifacts.values()) {
        if (artifact != null && artifact.isPrintable()) {
          return true;
        }
      }
      return false;
    }
    
    public boolean hasInterfacesNotNullContainingPrintableOperations(Map<String, Interface> interfaces) {
        if (interfaces == null) {
            return false;
        }
        for (Interface _interface : interfaces.values()) {
            boolean interfacePrintable = interfaceNotNullContainingPrintableOperations(_interface);
            if (interfacePrintable) {
                return true;
            }
        }
        return false;
    }

    public boolean interfaceNotNullContainingPrintableOperations(Interface _interface) {

        if (_interface != null) {
            for (Operation op : _interface.getOperations().values()) {
                if (op != null && op.isPrintable()) {
                    return true;
                }
            }
        }
        return false;
    }

    public String serializeInterfaces(Map<String, Interface> interfaces, String indentation) {
        String baseIndentation = indentation == null ? "" : indentation;
        String operationBaseIndentation = baseIndentation + "  ";
        String operationFieldsIndentation = operationBaseIndentation + "  ";
        String operationInputsIndentation = operationFieldsIndentation + "  ";

        StringBuilder builder = new StringBuilder();
        if (interfaces != null) {
            for (Entry<String, Interface> _interface : interfaces.entrySet()) {
                if (_interface.getValue() != null) {
                    builder.append(baseIndentation).append(_interface.getKey()).append(":\n");
                    for (Entry<String, Operation> operation : _interface.getValue().getOperations().entrySet()) {
                        if (operation.getValue() != null && operation.getValue().isPrintable()) {
                            builder.append(operationBaseIndentation).append(operation.getKey()).append(":\n");
                            String implementation = null;
                            if (operation.getValue().getImplementationArtifact() != null) {
                                implementation = operation.getValue().getImplementationArtifact().getArtifactRef();
                            }
                            if (!Strings.isNullOrEmpty(implementation)) {
                                builder.append(operationFieldsIndentation).append("implementation: ").append(renderScalar(implementation)).append("\n");
                            }
                            if (MapUtils.isNotEmpty(operation.getValue().getInputParameters())) {
                                builder.append(operationFieldsIndentation).append("inputs:\n");
                                for (Entry<String, IValue> input : operation.getValue().getInputParameters().entrySet()) {
                                    if (input.getValue() != null) {
                                        builder.append(operationInputsIndentation).append(input.getKey()).append(": ");
                                        if (isScalarPropertyValue(input.getValue())) {
                                            String value = ((ScalarPropertyValue) input.getValue()).getValue();
                                            builder.append(renderScalar(value)).append("\n");
                                        } else if (isFunctionPropertyValue(input.getValue()) || isConcatPropertyValue(input.getValue())) {
                                            String value = this.renderFunctionAndConcat((AbstractPropertyValue) input.getValue());
                                            builder.append("{ ").append(value).append(" }\n");
                                        } else {
                                            // can this happen??
                                            builder.append(renderScalar(String.valueOf(input.getValue()))).append("\n");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return builder.toString();
    }
      
    public String renderFunctionAndConcat(AbstractPropertyValue apv) {
      StringBuilder builder = new StringBuilder();
      String functionName;
      List<?> parameters = new ArrayList<>();
      if (isFunctionPropertyValue(apv)) {
        FunctionPropertyValue fpv = (FunctionPropertyValue) apv;
        functionName = fpv.getFunction();
        parameters = fpv.getParameters();
      } else if (isConcatPropertyValue(apv)) {
        ConcatPropertyValue cpv = (ConcatPropertyValue) apv;
        functionName = cpv.getFunction_concat();
        parameters = cpv.getParameters();
      } else {
        return builder.toString();
      }
      return builder
        .append(functionName)
        .append(": [ ")
        .append(getCsvToString(parameters, true))
        .append(" ]");
        .toString();
    }

    /**
     * Check if the list is not null, not empty and contains at least one not null value.
     * This function is recursive:
     * <ul>
     * <li>if a lis entry is a also a map, then we'll look for non null values in it (recursively).
     * <li>if a map entry is a collection, then will return true if the collection is not empty.
     * <li>if a map entry is a ScalarPropertyValue, then will return true if the value is not null.
     * </ul>
     */
    public boolean listIsNotEmptyAndContainsNotnullValues(List<?> l) {
      // TODO test if it works.
      if (!l.isEmpty()) {
        for (Object o : l) {
          if (o != null) {
            if (o instanceof Map<?, ?>) {
              if (mapIsNotEmptyAndContainsNotnullValues((Map<?, ?>) o)) {
                return true;
              }
            } else if (o instanceof Collection<?>) {
              if (!((Collection<?>) o).isEmpty()) {
                return true;
              }
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

    public boolean importsAreNotEmpty(Set<CSARDependency> dependencies) {
      if (collectionIsNotEmpty(dependencies)) {
        for (CSARDependency dependency : dependencies) {
          if (!dependency.isToscaDefinitionDependency()) {
            return true;
          }
        }
      }
      return false;
    }
}
