package alien4cloud.tosca.parser;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import alien4cloud.tosca.model.ArchiveRoot;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.OutputDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.tosca.parser.impl.ErrorCode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Utility class to help with parsing.
 */
public final class ParserUtils {

    public static final String OUTPUT_NAME = "dummy";
    public static final String TOSCA_DUMMY_TEMPLATE = "tosca_definitions_version: tosca_simple_yaml_1_0\ntopology_template:\n  outputs:\n    "
            + OUTPUT_NAME
            + ":\n      value: %s\n";

    /**
     * Utility to get a scalar.
     * 
     * @param node The node from which to get a scalar value.
     * @param context The parsing execution context in which to add errors in which to add errors in case the node is not a scalar node.
     * @return The Scalar value or null if the node is not a scalar node.
     */
    public static String getScalar(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            return ((ScalarNode) node).getValue().trim();
        }
        addTypeError(node, context.getParsingErrors(), "scalar");
        return null;
    }

    /**
     * Build a map while parsing a {@link MappingNode} assuming that all tuples are scalars (both key and value). Other entries are ignored but warned.
     * 
     * @param ignoredKeys ignore these keys (no warning for them !)
     */
    public static Map<String, String> parseStringMap(MappingNode mappingNode, ParsingContextExecution context, String... ignoredKeys) {
        Map<String, String> result = Maps.newHashMap();
        List<NodeTuple> mappingNodeValues = mappingNode.getValue();
        for (NodeTuple entry : mappingNodeValues) {
            if (!(entry.getKeyNode() instanceof ScalarNode)) {
                ParsingError err = new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRECOGNIZED_PROPERTY, "Parsing a MappingNode as a Map", entry
                        .getKeyNode().getStartMark(), "The key of this tuple should be a scalar", entry.getKeyNode().getEndMark(), "");
                context.getParsingErrors().add(err);
                continue;
            }
            if (!(entry.getValueNode() instanceof ScalarNode)) {
                if (!ArrayUtils.contains(ignoredKeys, getScalar(entry.getKeyNode(), context))) {
                    ParsingError err = new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRECOGNIZED_PROPERTY, "Parsing a MappingNode as a Map", entry
                            .getKeyNode().getStartMark(), "The value of this tuple should be a scalar", entry.getValueNode().getEndMark(),
                            ((ScalarNode) entry.getKeyNode()).getValue());
                    context.getParsingErrors().add(err);
                }
                continue;
            }
            // ok both key and value are scalar
            String k = ((ScalarNode) entry.getKeyNode()).getValue();
            String v = ((ScalarNode) entry.getValueNode()).getValue();
            result.put(k, v);
        }
        return result;
    }

    public static Object parse(Node node) {
        if (node == null) {
            return null;
        } else if (node instanceof ScalarNode) {
            return ((ScalarNode) node).getValue();
        } else if (node instanceof SequenceNode) {
            return parseSequence((SequenceNode) node);
        } else if (node instanceof MappingNode) {
            return parseMap((MappingNode) node);
        } else {
            throw new InvalidArgumentException("Unknown type of node " + node.getClass().getName());
        }
    }

    public static List<Object> parseSequence(SequenceNode sequenceNode) {
        List<Object> result = Lists.newArrayList();
        for (Node node : sequenceNode.getValue()) {
            result.add(parse(node));
        }
        return result;
    }

    public static Map<String, Object> parseMap(MappingNode mappingNode) {
        Map<String, Object> result = Maps.newHashMap();
        for (NodeTuple entry : mappingNode.getValue()) {
            String key = ((ScalarNode) entry.getKeyNode()).getValue();
            result.put(key, parse(entry.getValueNode()));
        }
        return result;
    }

    /**
     * Add an invalid type {@link ParsingError} to the given parsing errors list.
     * 
     * @param node The node that is causing the type error.
     * @param parsingErrors The parsing errors in which to add the error.
     * @param expectedType The type that was actually expected.
     */
    public static void addTypeError(Node node, List<ParsingError> parsingErrors, String expectedType) {
        parsingErrors.add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Invalid type syntax", node.getStartMark(), "Expected the type to match tosca type", node
                .getEndMark(), expectedType));
    }

    public static Object parsePropertyValue(ToscaSimpleParser toscaParser, Object propertyValue ) throws ParsingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (propertyValue instanceof Map) {
            Map<String, Object> propertyValueParsed = ((Map<String, Object>) propertyValue);
            if (!propertyValueParsed.containsKey("function") && !propertyValueParsed.containsKey("parameters")) {
                Map<String, Object> result = new HashMap<>();
                for (Map.Entry<String, Object> o : propertyValueParsed.entrySet()) {
                    Object parsedPropertyValue = parsePropertyValue(toscaParser, o.getValue());
                    result.put(o.getKey(), parsedPropertyValue);
                }
                return result;
            } else {
                return functionFromMap(propertyValue);
            }
        } else if (propertyValue instanceof Collection) {
            Collection propertyValueParsed = ((Collection) propertyValue);
            Collection result = new ArrayList();
            for (Object o: propertyValueParsed) {
                Object item = parsePropertyValue(toscaParser, o);
                result.add(item);
            }
            return result;
        } else if (propertyValue instanceof String) {
            return getUpdateProperty(toscaParser, propertyValue.toString());
        } else if (propertyValue instanceof Number || propertyValue instanceof Boolean) {
            return new ScalarPropertyValue(propertyValue.toString());
        } else {
            return propertyValue;
        }
    }

    protected static Object functionFromMap(Object functionObj) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (functionObj instanceof Map) {
            FunctionPropertyValue fpv = new FunctionPropertyValue();
            Map<String, Object> functionObjMap = ((Map<String, Object>) functionObj);
            fpv.setFunction(functionObjMap.get("function").toString());
            Collection parameters = (Collection) functionObjMap.get("parameters");
            List<Object> result = new ArrayList();
            for (Object o: parameters) {
                Object item = functionFromMap(o);
                result.add(item);
            }
            fpv.setParameters(result);
            return fpv;
        } else if (functionObj instanceof Collection) {
            Collection functionObjArr = ((Collection) functionObj);
            List<Object> result = new ArrayList();
            for (Object o: functionObjArr) {
                Object item = functionFromMap(o);
                result.add(item);
            }
            return result;
        } else if (functionObj instanceof Number || functionObj instanceof Boolean) {
            Class<?> clazz = Class.forName(functionObj.getClass().getName());
            Constructor<?> ctor = clazz.getConstructor(String.class);
            return ctor.newInstance(new Object[] { functionObj });
        } else {
            return functionObj.toString();
        }

    }

    protected static AbstractPropertyValue getUpdateProperty(ToscaSimpleParser toscaParser, String propertyValue) throws ParsingException {
        ParsingResult<ArchiveRoot> pr = toscaParser.parse(
                new ByteArrayInputStream(String.format(TOSCA_DUMMY_TEMPLATE, propertyValue).getBytes(StandardCharsets.UTF_8)), null);
        OutputDefinition odNew = pr.getResult().getTopology().getOutputs().get(OUTPUT_NAME);
        return odNew.getValue();
    }



}