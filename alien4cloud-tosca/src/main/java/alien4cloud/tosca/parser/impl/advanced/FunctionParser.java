package alien4cloud.tosca.parser.impl.advanced;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;

@Component
public class FunctionParser  implements INodeParser<FunctionPropertyValue> {

  @Override
  public FunctionPropertyValue parse(Node node, ParsingContextExecution context) {
    FunctionPropertyValue result = null;
    if (node instanceof MappingNode) {
      List<Object> parameters = null; 
      MappingNode mn = (MappingNode) node;
      Node fNameNode = mn.getValue().get(0).getKeyNode();
      if (fNameNode instanceof ScalarNode) {
        result = new FunctionPropertyValue();
        result.setFunction(((ScalarNode)fNameNode).getValue());
        Node paramNodes = mn.getValue().get(0).getValueNode();
        if (paramNodes instanceof SequenceNode) {
          parameters = new ArrayList<>();
          List<Node> params = ((SequenceNode) paramNodes).getValue();
          for (Node param: params) {
            // Check each parameter
            // A TOSCA function only allows another function or a scalar as params
            if (param instanceof MappingNode) {
              // Another mapping, it should be a function
              parameters.add(this.parse(param, context));
            } else if (param instanceof ScalarNode) {
              // No more recursion needed, it's a basic type
              parameters.add(((ScalarNode)param).getValue());
            } else if (param instanceof SequenceNode) {
              // No more recursion needed, it's an array
              parameters.add(((SequenceNode)param).getValue());
            } else
              context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.INVALID_TOSCA_FUNCTION_DECLARATION, null,
                  mn.getStartMark(), " function parameter type unknown for TOSCA function " + result.getFunction(), mn.getEndMark(), null));
          }
          result.setParameters(parameters);
        } else if (paramNodes instanceof ScalarNode) { // get_input doesn't have a list of parameters
          parameters = new ArrayList<>();
          parameters.add(((ScalarNode)paramNodes).getValue());  
          result.setParameters(parameters);        
        } else
          context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.INVALID_TOSCA_FUNCTION_DECLARATION, null,
              mn.getStartMark(), " function parameters must be declared as array in the TOSCA function " + result.getFunction(), mn.getEndMark(), null));
      } else {
        context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.INVALID_TOSCA_FUNCTION_DECLARATION, null,
            mn.getStartMark(), " function name expected at this position instead of " + fNameNode.toString() , mn.getEndMark(), null));
      }
    }
    return result;
  }

}
