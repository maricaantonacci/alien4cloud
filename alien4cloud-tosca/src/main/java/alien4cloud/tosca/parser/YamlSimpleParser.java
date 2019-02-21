package alien4cloud.tosca.parser;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

@AllArgsConstructor
@Slf4j
public class YamlSimpleParser<T> extends YamlParser<T> {
    private INodeParser<T> nodeParser;

    @Override
    protected INodeParser<T> getParser(Node rootNode, ParsingContextExecution context) throws ParsingException {
        return nodeParser;
    }
}