package alien4cloud.tosca.parser.impl.v10.normative;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.MapParser;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component("abstractPropertyValueParser_v10_normative")
public class AbstractPropertyValueParser implements INodeParser<Object> {

  private static final ThreadLocal<Boolean> NESTED = new ThreadLocal<>();

  @Resource
  private BaseParserFactory baseParserFactory;

  private ListParser<Object> listPropertyParser;
  private MapParser<Object> mapPropertyParser;

  private final Set<String> allowedFunctions;

  public AbstractPropertyValueParser(Set<String> allowedFunctions) {
    this.allowedFunctions = Objects.requireNonNull(allowedFunctions);
  }

  @PostConstruct
  public void init() {
    listPropertyParser = baseParserFactory.getListParser(this, "list of AbstractPropertyValue");
    mapPropertyParser = baseParserFactory.getMapParser(this, "map of AbstractPropertyValue");
  }

  @Override
  public Object parse(Node node, ParsingContextExecution context) {
      if (node instanceof ScalarNode) {
        String parsedValue = ParserUtils.getScalar(node, context);
        if (parsedValue == null || isNested()) {
          return parsedValue;
        } else {
          return new ScalarPropertyValue(parsedValue);
        }
      } else if (node instanceof SequenceNode) {
        List<Object> parsedValues = parseNested(node, context, listPropertyParser::parse);
        if (parsedValues == null || isNested()) {
          return parsedValues;
        } else {
          return new ListPropertyValue(parsedValues);
        }
      } else if (node instanceof MappingNode) {
        List<NodeTuple> nodeTuples = ((MappingNode) node).getValue();
        String functionName = extractOptionalFunctionName(nodeTuples);
        if (functionName != null) {
          if (!allowedFunctions.contains(functionName)) {
            // some functions are disallowed in some places
            context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR,
                ErrorCode.INVALID_TOSCA_FUNCTION_DECLARATION, null,
                node.getStartMark(), "Function " + functionName + " not allowed in this position",
                node.getEndMark(), functionName));
            return null;
          }
          Node parametersNode = nodeTuples.get(0).getValueNode();
          List<Object> params = parseNested(parametersNode, context, listPropertyParser::parse);
          if (params == null) {
            return null;
          } else {
            return new FunctionPropertyValue(functionName, params);
          }
        } else {
          Map<String, Object> parsedValues = parseNested(node, context, mapPropertyParser::parse);
          if (parsedValues == null || isNested()) {
            return parsedValues;
          } else {
            return new ComplexPropertyValue(parsedValues);
          }
        }
      } else {
        context.getParsingErrors().add(
            new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.INVALID_TOSCA_FUNCTION_DECLARATION,
                null,
                node.getStartMark(), "Expecting a Scalar or a Sequence or a Mapping node",
                node.getEndMark(), null));
        return null;
      }
  }

  private String extractOptionalFunctionName(List<NodeTuple> nodeTuples) {
    return Optional
              // we have a function name
              .of(nodeTuples)
              // if the mapping node is with only one element
              .filter(nt -> nodeTuples.size() == 1)
              .map(nt -> nodeTuples.get(0).getKeyNode())
              // the key node is scalar
              .filter(ScalarNode.class::isInstance)
              .map(keyNode -> ((ScalarNode) keyNode).getValue())
              // and its value is a normative function name
              .filter(ToscaFunctionConstants.normativeFunctionsV10::contains)
              .orElse(null);
  }

  private static boolean isNested() {
    return NESTED.get() != null;
  }

  private <T> T parseNested(Node node, ParsingContextExecution context, BiFunction<Node, ParsingContextExecution, T> parser) {
    boolean isNotAlreadyNested = !isNested();
    try {
      if (isNotAlreadyNested) {
        NESTED.set(true);
      }
      return parser.apply(node, context);
    } finally {
      if (isNotAlreadyNested) {
        NESTED.remove();
      }
    }
  }
}
