package alien4cloud.tosca.parser.mapping.generator;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import alien4cloud.tosca.parser.impl.base.ListParser;

import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * Build Mapping target for map.
 */
@Component
public class AbstractPropertyValueMappingBuilder implements IMappingBuilder {
    private static final String VALUE = "property_value";
    private static final String FUNCTIONS = "functions";
    private static final String PARSER = "parser";

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public String getKey() {
        return VALUE;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        String mappingTarget = "";
        Set<String> allowedFunctions = new HashSet<>();
        String defaultParserName = null;

        for (NodeTuple tuple : mappingNode.getValue()) {
            String tupleKey = ParserUtils.getScalar(tuple.getKeyNode(), context);
            if (FUNCTIONS.equals(tupleKey)) {
                allowedFunctions = ((SequenceNode) tuple.getValueNode())
                    .getValue()
                    .stream()
                    .map(node -> ParserUtils.getScalar(node, context))
                    .collect(Collectors.toSet());
            } else if (PARSER.equals(tupleKey)) {
                defaultParserName = ParserUtils.getScalar(tuple.getValueNode(), context);
            }
        }
        INodeParser<?> defaultParser = (INodeParser) applicationContext.getBean(defaultParserName, allowedFunctions);
        return new MappingTarget(mappingTarget, defaultParser);
    }
}
