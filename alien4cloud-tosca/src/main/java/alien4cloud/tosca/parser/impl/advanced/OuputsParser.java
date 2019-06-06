package alien4cloud.tosca.parser.impl.advanced;

import java.util.*;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.OutputDefinition;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OuputsParser implements INodeParser<Void> {
    @Override
    public Void parse(Node node, ParsingContextExecution context) {
        Topology topology = (Topology) context.getParent();

        if (!(node instanceof MappingNode)) {
            context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.YAML_MAPPING_NODE_EXPECTED, null, node.getStartMark(), null,
                    node.getEndMark(), null));
            return null;
        }
        MappingNode mappingNode = (MappingNode) node;

        Map<String, Set<String>> outputAttributes = new HashMap<>();
        Map<String, Set<String>> outputProperties = new HashMap<>();
        Map<String, Map<String, Set<String>>> ouputCapabilityProperties = new HashMap<>();
        Map<String, OutputDefinition> outputsByName = new HashMap<>();

        List<NodeTuple> children = mappingNode.getValue();
        for (NodeTuple child : children) {
            String name = ParserUtils.getScalar(child.getKeyNode(), context);
            Node childValueNode = child.getValueNode();
            if (!(childValueNode instanceof MappingNode)) {
                // not a mapping jut ignore the entry
                continue;
            }
            for (NodeTuple childChild : ((MappingNode) childValueNode).getValue()) {
                String description = null;
                Node outputValueNode = null;
                if (childChild.getKeyNode() instanceof ScalarNode && ((ScalarNode) childChild.getKeyNode()).getValue().equals("description")) {
                    description = ((ScalarNode) childChild.getValueNode()).getValue();
                }
                if (childChild.getKeyNode() instanceof ScalarNode && ((ScalarNode) childChild.getKeyNode()).getValue().equals("value")) {
                    // we are only interested by the 'value' node
                    outputValueNode = childChild.getValueNode();
                    // now we have to parse this node
                    INodeParser<?> p = context.getRegistry().get("output_property");
                    Object value = p.parse(outputValueNode, context);
                    extractMaps(value, outputAttributes, outputProperties , ouputCapabilityProperties);
                }

                outputsByName = addOutput(name, outputValueNode, description, context, outputsByName);
            }

        }

        topology.setOutputProperties(outputProperties);
        topology.setOutputAttributes(outputAttributes);
        topology.setOutputCapabilityProperties(ouputCapabilityProperties);
        topology.setOutputs(outputsByName);

        return null;
    }

    protected void extractMaps(Object value,
        Map<String, Set<String>> outputAttributes,
        Map<String, Set<String>> outputProperties,
        Map<String, Map<String, Set<String>>> ouputCapabilityProperties) {
        if (value instanceof FunctionPropertyValue) {
            FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) value;
            String functionName = functionPropertyValue.getFunction();
            List<Object> params = functionPropertyValue.getParameters();
            if ("concat".equals(functionName)) {
                params.forEach(p -> extractMaps(p, outputAttributes, outputProperties, ouputCapabilityProperties));
            } else if ("token".equals(functionName)){
                extractMaps(params.get(0), outputAttributes, outputProperties, ouputCapabilityProperties);
            } else if (params.size() == 2) {
                // TODO: should we check they exist ?
                switch (functionName) {
                    case "get_attribute":
                        // we need exactly 2 params to be able to do the job : node name & property or attribute name
                        String nodeTemplateName = params.get(0).toString();
                        String nodeTemplatePropertyOrAttributeName = params.get(1).toString();
                        addToMapOfSet(nodeTemplateName, nodeTemplatePropertyOrAttributeName, outputAttributes);
                        break;
                    case "get_property":
                        // we need exactly 2 params to be able to do the job : node name & property or attribute name
                        nodeTemplateName = params.get(0).toString();
                        nodeTemplatePropertyOrAttributeName = params.get(1).toString();
                        addToMapOfSet(nodeTemplateName, nodeTemplatePropertyOrAttributeName, outputProperties);
                        break;
                }
            } else if (params.size() == 3 && functionName.equals("get_property")) {
                // in case of 3 parameters we only manage capabilities outputs for the moment
                String nodeTemplateName = params.get(0).toString();
                String capabilityName = params.get(1).toString();
                String propertyName = params.get(2).toString();
                addToMapOfMapOfSet(nodeTemplateName, capabilityName, propertyName, ouputCapabilityProperties);
            }
        }
    }

    protected <T> Map<String, OutputDefinition> addOutput(String name, Node valueNode,
        String description, ParsingContextExecution context,
        Map<String, OutputDefinition> outputsByName) {
        if (outputsByName == null) {
            outputsByName = new HashMap<>();
        }
        INodeParser<?> p = context.getRegistry().get("output_property");
        AbstractPropertyValue outputValue = (AbstractPropertyValue) p.parse(valueNode, context);
        if (outputValue != null) {
            OutputDefinition output = new OutputDefinition(name, description, outputValue);
            outputsByName.put(name, output);
        }
      return outputsByName;
    }

    private Map<String, Set<String>> addToMapOfSet(String key, String value, Map<String, Set<String>> map) {
        if (map == null) {
            map = new HashMap<String, Set<String>>();
        }
        Set<String> set = map.get(key);
        if (set == null) {
            set = new HashSet<String>();
            map.put(key, set);
        }
        set.add(value);
        return map;
    }

    private Map<String, Map<String, Set<String>>> addToMapOfMapOfSet(String key1, String key2, String value, Map<String, Map<String, Set<String>>> map) {
        if (map == null) {
            map = new HashMap<String, Map<String, Set<String>>>();
        }
        Map<String, Set<String>> map1 = map.get(key1);
        map.put(key1, addToMapOfSet(key2, value, map1));
        return map;
    }

}