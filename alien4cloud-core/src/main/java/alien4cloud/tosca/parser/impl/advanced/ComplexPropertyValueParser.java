package alien4cloud.tosca.parser.impl.advanced;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.util.List;
import java.util.Map;

import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.mapping.DefaultParser;

@Component
public class ComplexPropertyValueParser extends DefaultParser<AbstractPropertyValue> {

    @Override
    public AbstractPropertyValue parse(Node node, ParsingContextExecution context) {
        AbstractPropertyValue parsedValue;
        if (node instanceof MappingNode) {
            Map<String, Object> result = Maps.newHashMap();
            for (NodeTuple innernode : ((MappingNode) node).getValue()) {
                AbstractPropertyValue parsedInnerValue = parse(innernode.getValueNode(), context);
                result.put(ParserUtils.getScalar(innernode.getKeyNode(), context), parsedInnerValue);
            }
            parsedValue = new ComplexPropertyValue(result);
        } else if (node instanceof SequenceNode) {
            List<Object> result = Lists.newArrayList();
            for (Node innernode : ((SequenceNode) node).getValue()) {
                AbstractPropertyValue parsedInnerValue = parse(innernode, context);
                result.add(parsedInnerValue);
            }
            parsedValue = new ListPropertyValue(result);
        } else if (node instanceof ScalarNode) {
            parsedValue = new ScalarPropertyValue(((ScalarNode) node).getValue());
        } else {
            throw new InvalidArgumentException("Do not expect other node than MappingNode or SequenceNode here " + node.getClass().getName());
        }
        parsedValue.setPrintable(true);
        return parsedValue;
    }
}
