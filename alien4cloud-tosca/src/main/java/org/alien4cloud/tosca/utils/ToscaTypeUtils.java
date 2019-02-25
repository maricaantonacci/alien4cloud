package org.alien4cloud.tosca.utils;

import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilities to process tosca types.
 */
public class ToscaTypeUtils {
  
  /**
   * A set containing the names of TOSCA all TOSCA functions defined in the version 1.1 of the Standard
   */
  public static final Set<String> TOSCA_FUNCTIONS = 
      Stream.of("get_input", "get_attribute", "concat", "get_property", "get_operation_output",
          "get_nodes_of_type", "get_artifact", "get_property", "token")
        .collect(Collectors.toSet());


    /**
     * Check whether the node type is equals or derived from the given type name
     *
     * @param inheritableToscaType the node type
     * @param type the type name
     * @return true if the node type is equals or derived from the given type name
     */
    public static boolean isOfType(AbstractInheritableToscaType inheritableToscaType, String type) {
        return inheritableToscaType != null && (inheritableToscaType.getElementId().equals(type)
                || inheritableToscaType.getDerivedFrom() != null && inheritableToscaType.getDerivedFrom().contains(type));
    }

    /**
     * Verify that the given <code>type</code> is or inherits the given <code>expectedType</code>.
     */
    public static boolean isOfType(String type, List<String> typeHierarchy, String expectedType) {
        return expectedType.equals(type) || (typeHierarchy != null && typeHierarchy.contains(expectedType));
    }
    
    /**
     * Check if a node respects the definition of a TOSCA function; Be aware that this method only checks
     * the first level of a TOSCA function definition. If you have a composed TOSCA function (one or
     * more params are also TOSCA functions), then the validation is done by the parser itself
     * @param node The node that we want to check
     * @return true if the node is a valid TOSCA function, false otherwise
     */
    public static boolean isFunctionNode(Node node) {
      if (node instanceof MappingNode) {
        Node functionNameNode = ((MappingNode) node).getValue().get(0).getKeyNode();
        if (functionNameNode instanceof ScalarNode) {
          String functionName = ((ScalarNode)functionNameNode).getValue().toLowerCase();
          return TOSCA_FUNCTIONS.contains(functionName);
        } else
          return false;
      } else 
        return false;
    }
}