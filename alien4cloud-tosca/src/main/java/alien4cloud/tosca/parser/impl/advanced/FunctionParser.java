package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.elasticsearch.common.inject.Inject;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.*;

import java.util.*;

@Component
public class FunctionParser implements INodeParser<AbstractPropertyValue> {


    @Override
    public AbstractPropertyValue parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            MappingNode mn = (MappingNode) node;
            if (mn.getValue().size() == 1) {
                NodeTuple function = mn.getValue().get(0);
                if (function.getKeyNode() instanceof ScalarNode) {
                    String fName = ((ScalarNode) function.getKeyNode()).getValue();
                    if (ToscaFunctionConstants.normativeFunctionsV10.contains(fName.toLowerCase())) {
                        if (function.getValueNode() instanceof SequenceNode) {
                            List<Object> arguments = new ArrayList<>();
                            List<Node> args = ((SequenceNode) function.getValueNode()).getValue();
                            for (Node arg: args) {
                                AbstractPropertyValue argument = this.parse(arg, context);
                                arguments.add(argument);
                            }
                            return new FunctionPropertyValue(fName, arguments);
                        } else if (function.getValueNode() instanceof ScalarNode) {
                            return new FunctionPropertyValue(fName, Arrays.asList(((ScalarNode) function.getValueNode()).getValue()));
                        } else
                            ParserUtils.addTypeError(node, context.getParsingErrors(), "Function  " + fName + " not supported");
                    } else
                        ParserUtils.addTypeError(node, context.getParsingErrors(), "Function  " + fName + " not supported");
                } else {
                    ParserUtils.addTypeError(node, context.getParsingErrors(), "Only scalar nodes are supported as keys; type " + function.getKeyNode().getType().getName() + " is not supported");
                }
            } else {
                Map<String, Object> cpv = new HashMap<>();
                for (NodeTuple nt: mn.getValue()) {
                    if (nt.getKeyNode() instanceof ScalarNode) {
                        String key = ((ScalarNode) nt.getKeyNode()).getValue();
                        AbstractPropertyValue value = this.parse(nt.getValueNode(), context);
                        cpv.put(key, value);
                    } else {
                        ParserUtils.addTypeError(node, context.getParsingErrors(),
                                "Only scalar nodes are supported as keys; type " + nt.getKeyNode().getType().getName() + " is not supported");
                    }
                }
                return new ComplexPropertyValue(cpv);
            }

        } else if (node instanceof SequenceNode) {
            List<Node> sn = ((SequenceNode) node).getValue();
            List<Object> lpv = new ArrayList<>();
            for (Node n: sn) {
                AbstractPropertyValue element = this.parse(n, context);
                lpv.add(element);
            }
            return  new ListPropertyValue(lpv);
        } else if (node instanceof ScalarNode) {
            ScalarNode sn = (ScalarNode) node;
            return new ScalarPropertyValue(sn.getValue());
        } else
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Node with type " + node.getType().getName() + " not supported");
        return null;
    }
}
