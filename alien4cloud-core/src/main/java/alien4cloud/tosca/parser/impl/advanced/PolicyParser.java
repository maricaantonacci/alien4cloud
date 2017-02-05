package alien4cloud.tosca.parser.impl.advanced;

import com.google.common.collect.Lists;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.topology.AbstractPolicy;
import alien4cloud.model.topology.PlacementPolicy;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultDeferredParser;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import javax.annotation.Resource;

import java.util.List;
import java.util.Map;

@Component
public class PolicyParser extends DefaultDeferredParser<AbstractPolicy> {

    @Resource
    private ScalarParser scalarParser;

    @Override
    public AbstractPolicy parse(Node node, ParsingContextExecution context) {
        if (!(node instanceof MappingNode)) {
            // we expect a MappingNode
            context.getParsingErrors().add(new ParsingError(ErrorCode.YAML_MAPPING_NODE_EXPECTED, null, node.getStartMark(), null, node.getEndMark(), null));
            return null;
        }
        MappingNode mappingNode = (MappingNode) node;
        if (CollectionUtils.size(mappingNode.getValue()) != 1) {
            context.getParsingErrors().add(new ParsingError(ErrorCode.SYNTAX_ERROR, null, mappingNode.getStartMark(), null, mappingNode.getEndMark(), null));
            return null;
        }
        String policyName = scalarParser.parse(mappingNode.getValue().get(0).getKeyNode(), context);
        Node policyValuesNode = mappingNode.getValue().get(0).getValueNode();
        if (!(policyValuesNode instanceof MappingNode)) {
            // we expect a MappingNode
            context.getParsingErrors().add(new ParsingError(ErrorCode.YAML_MAPPING_NODE_EXPECTED, null, policyValuesNode.getStartMark(), null, policyValuesNode.getEndMark(), null));
            return null;
        }
        MappingNode policyValuesMappingNode = (MappingNode) policyValuesNode;
        AbstractPolicy policy = null;
        String description = null;
        Map<String, AbstractPropertyValue> properties = null;
        List<String> targets = Lists.newArrayList();
        for (NodeTuple valueNodeTuple : policyValuesMappingNode.getValue()) {
            String key = scalarParser.parse(valueNodeTuple.getKeyNode(), context);
            switch (key) {
            case "type":
                String type = scalarParser.parse(valueNodeTuple.getValueNode(), context);
                switch (type) {
                case PlacementPolicy.PLACEMENT_POLICY:
                    policy = new PlacementPolicy();
                    break;
                default:
                    context.getParsingErrors().add(new ParsingError(ErrorCode.UNKOWN_POLICY, null, valueNodeTuple.getKeyNode().getStartMark(), null,
                            valueNodeTuple.getKeyNode().getEndMark(), null));
                    return null;
                }
                break;
            case "properties":
                INodeParser<AbstractPropertyValue> propertyValueParser = context.getRegistry().get("node_template_property");
                MapParser<AbstractPropertyValue> mapParser = new MapParser<>(propertyValueParser, "node_template_property");
                properties = mapParser.parse(valueNodeTuple.getValueNode(), context);
                break;
            case "targets":
                ListParser<String> listParser = new ListParser<>(scalarParser, "string");
                targets.addAll(listParser.parse(valueNodeTuple.getValueNode(), context));
                break;
            case "description":
                description = scalarParser.parse(valueNodeTuple.getValueNode(), context);
                break;
            default:
                context.getParsingErrors().add(new ParsingError(ErrorCode.UNRECOGNIZED_PROPERTY, null, valueNodeTuple.getKeyNode().getStartMark(), null,
                        valueNodeTuple.getKeyNode().getEndMark(), null));
                return null;
            }
        }
        if (policy == null) {
            context.getParsingErrors().add(new ParsingError(ErrorCode.SYNTAX_ERROR, null, node.getStartMark(), null,
                    node.getEndMark(), null));
            return null;
        }
        policy.setDescription(description);
        policy.setName(policyName);
        if (CollectionUtils.isNotEmpty(targets)) {
            final ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
            for (String target : targets) {
                if (!archiveRoot.getTopology().getNodeTemplates().containsKey(target)) {
                    context.getParsingErrors().add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Policy parsing", node.getStartMark(), "Unknown target",
                            node.getEndMark(), target));
                    return null;
                }
            }
            policy.setTargets(targets);
        }
        if (MapUtils.isNotEmpty(properties)) {
            policy.setProperties(properties);
        }
        return policy;
    }

}