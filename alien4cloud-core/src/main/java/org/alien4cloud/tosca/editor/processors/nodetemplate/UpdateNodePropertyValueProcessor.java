package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.annotation.Resource;

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.ToscaSimpleParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alien4cloud.tosca.editor.exception.PropertyValueException;
import org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodePropertyValueOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;

import org.alien4cloud.tosca.utils.TopologyUtils;
import alien4cloud.tosca.context.ToscaContext;
import org.alien4cloud.tosca.exceptions.ConstraintFunctionalException;
import alien4cloud.utils.services.PropertyService;
import lombok.extern.slf4j.Slf4j;

/**
 * Process an update node property value operation against the topology in the edition context.
 */
@Slf4j
@Component
public class UpdateNodePropertyValueProcessor implements IEditorOperationProcessor<UpdateNodePropertyValueOperation> {
    @Resource
    private PropertyService propertyService;

    public static final String OUTPUT_NAME = "dummy";
    public static final String TOSCA_DUMMY_TEMPLATE = "tosca_definitions_version: tosca_simple_yaml_1_0\ntopology_template:\n  outputs:\n    "
            + OUTPUT_NAME
            + ":\n      value: %s\n";

    @Autowired
    protected ToscaSimpleParser toscaParser;

    @Override
    public void process(Csar csar, Topology topology, UpdateNodePropertyValueOperation operation) {
        Map<String, NodeTemplate> nodeTemplates = TopologyUtils.getNodeTemplates(topology);
        NodeTemplate nodeTemp = TopologyUtils.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);
        String propertyName = operation.getPropertyName();
        Object propertyValue = operation.getPropertyValue();

        NodeType node = ToscaContext.getOrFail(NodeType.class, nodeTemp.getType());

        PropertyDefinition propertyDefinition = node.getProperties().get(propertyName);
        if (propertyDefinition == null) {
            throw new NotFoundException(
                    "Property <" + propertyName + "> doesn't exists for node <" + operation.getNodeName() + "> of type <" + nodeTemp.getType() + ">");
        }

        log.debug("Updating property [ {} ] of the Node template [ {} ] from the topology [ {} ]: changing value from [{}] to [{}].", propertyName,
                operation.getNodeName(), topology.getId(), nodeTemp.getProperties().get(propertyName), propertyValue);

        try {
            propertyValue = parsePropertyValue(propertyValue);
            propertyService.setPropertyValue(nodeTemp, propertyDefinition, propertyName, propertyValue);
        } catch (ConstraintFunctionalException e) {
            throw new PropertyValueException("Error when setting node " + operation.getNodeName() + " property.", e, propertyName, propertyValue);
        } catch (ParsingException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new PropertyValueException("Error when setting node " + operation.getNodeName() + " property.",
                    new ConstraintFunctionalException(e.getMessage(), e), propertyName, propertyValue);
        }
    }

    protected Object parsePropertyValue(Object propertyValue ) throws ParsingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (propertyValue instanceof Map) {
            Map<String, Object> propertyValueParsed = ((Map<String, Object>) propertyValue);
            if (!propertyValueParsed.containsKey("function") && !propertyValueParsed.containsKey("parameters")) {
                Map<String, Object> result = new HashMap<>();
                for (Map.Entry<String, Object> o : propertyValueParsed.entrySet()) {
                    Object parsedPropertyValue = parsePropertyValue(o.getValue());
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
                Object item = parsePropertyValue(o);
                result.add(item);
            }
            return result;
        } else if (propertyValue instanceof String) {
            return getUpdateProperty(propertyValue.toString());
        } else if (propertyValue instanceof Number || propertyValue instanceof Boolean) {
            return new ScalarPropertyValue(propertyValue.toString());
        } else {
            return propertyValue;
        }
    }

    protected Object functionFromMap(Object functionObj) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
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

    protected AbstractPropertyValue getUpdateProperty(String propertyValue) throws ParsingException {
        ParsingResult<ArchiveRoot> pr = toscaParser.parse(
                new ByteArrayInputStream(String.format(TOSCA_DUMMY_TEMPLATE, propertyValue).getBytes(StandardCharsets.UTF_8)), null);
        OutputDefinition odNew = pr.getResult().getTopology().getOutputs().get(OUTPUT_NAME);
        return odNew.getValue();
    }
}