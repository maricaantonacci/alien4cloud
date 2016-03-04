package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.util.List;
import java.util.Map;

import alien4cloud.csar.services.CsarService;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultParser;

@Component
public class ImportParser extends DefaultParser<CSARDependency> {
    @Resource
    private CsarService csarService;
    @Resource
    private ScalarParser scalarParser;

    @Override
    public CSARDependency parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            return parseMappingImport((MappingNode) node, context);
        } else if (node instanceof ScalarNode) {
            return parseScalarImport((ScalarNode) node, context);
        } else {
            context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR, "Import definition is not valid",
                    node.getStartMark(), null, node.getEndMark(), "Import"));
            return null;
        }
    }

    private void checkCSARDependency(CSARDependency dependency, Node node, ParsingContextExecution context) {
        Csar csar = csarService.getIfExists(dependency.getName(), dependency.getVersion());
        if (csar == null) {
            // error is not a blocker, as long as no type is missing we just mark it as a warning.
            context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.MISSING_DEPENDENCY, "Import definition is not valid",
                    node.getStartMark(), "Specified dependency is not found in Alien 4 Cloud repository.", node.getEndMark(), "Import"));
        }
    }

    private CSARDependency parseMappingImport(MappingNode node, ParsingContextExecution context) {
        CSARDependency dependency;
        List<NodeTuple> mappingNodeValues = ((MappingNode) node).getValue();
        if (mappingNodeValues.size() != 1) {
            context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR, "Import definition is not valid",
                    node.getStartMark(), null, node.getEndMark(), "Import"));
            return null;
        }
        NodeTuple mappingNodeValue = mappingNodeValues.get(0);
        String name = ParserUtils.getScalar(mappingNodeValue.getKeyNode(), context);
        if (name != null && !name.trim().isEmpty()) {
            dependency = new CSARDependency(name);
            if (mappingNodeValue.getValueNode() instanceof ScalarNode) {
                dependency.setFile(ParserUtils.getScalar(mappingNodeValue.getValueNode(), context));
                checkCSARDependency(dependency, node, context);
                return dependency;
            } else if (mappingNodeValue.getValueNode() instanceof MappingNode) {
                Map<String, String> values = ParserUtils.parseStringMap((MappingNode) mappingNodeValue.getValueNode(), context);
                dependency.setFile(values.get("file"));
                dependency.setRepository(values.get("repository"));
                checkCSARDependency(dependency, node, context);
                return dependency;
            } else {
                context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR, "Import definition is not valid",
                        node.getStartMark(), null, node.getEndMark(), "Import"));
                return null;
            }
        }
        return null;
    }

    private CSARDependency parseScalarImport(ScalarNode node, ParsingContextExecution context) {
        String valueAsString = scalarParser.parse(node, context);
        if (valueAsString == null || valueAsString.trim().isEmpty()) {
            return null;
        }
        String[] dependencyStrs = valueAsString.split(":");
        if (dependencyStrs.length == 2) {
            CSARDependency dependency = new CSARDependency(dependencyStrs[0], dependencyStrs[1]);
            checkCSARDependency(dependency, node, context);
            return dependency;
        } else {
            context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR, "Import definition is not valid",
                    node.getStartMark(), "Dependency should be specified as name:version", node.getEndMark(), "Import"));
        }
        return null;
    }
}