package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

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
        if (node instanceof MappingNode) {
            return new ComplexPropertyValue(ParserUtils.parseMap((MappingNode) node));
        } else if (node instanceof SequenceNode) {
            return new ListPropertyValue(ParserUtils.parseSequence((SequenceNode) node));
        } else if (node instanceof ScalarNode) {
            return new ScalarPropertyValue(((ScalarNode) node).getValue());
        } else {
            throw new InvalidArgumentException("Do not expect other node than MappingNode or SequenceNode here " + node.getClass().getName());
        }
    }
}
