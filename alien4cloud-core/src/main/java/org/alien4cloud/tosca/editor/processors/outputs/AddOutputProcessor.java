package org.alien4cloud.tosca.editor.processors.outputs;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidNameException;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.operations.outputs.AddOutputOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.OutputDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class AddOutputProcessor extends AbstractOutputProcessor<AddOutputOperation> {

    public static final String TOSCA_OUTPUT_TEMPLATE = "tosca_definitions_version: tosca_simple_yaml_1_0\ntopology_template:\n  outputs:\n    %s:\n      value: %s\n";

    @Autowired
    protected ToscaSimpleParser toscaParser;

    @Override
    protected boolean create() {
        return true; // create the outputs map if null in the topology.
    }

    @Override
    protected void processOutputOperation(Csar csar, Topology topology, AddOutputOperation operation, Map<String, OutputDefinition> outputs) {
        if (operation.getOutputName() == null || operation.getOutputName().isEmpty() || !operation.getOutputName().matches("\\w+")) {
            throw new InvalidNameException("newOutputName", operation.getOutputName(), "\\w+");
        }

        if (outputs.containsKey(operation.getOutputName())) {
            throw new AlreadyExistException("An output with the id " + operation.getOutputName() + "already exist in the topology " + topology.getId());
        }

        OutputDefinition od = operation.getOutputDefinition();
        // Check if the value is a function
        if (od.getValue() instanceof ScalarPropertyValue) {
//            Yaml yaml = new Yaml();

//            ParsingContextExecution.getRegistry().get(parserName);
//            ParsingContextExecution.get();
//            context.init();
//            context.setRegistry(Maps.newHashMap());
//                INodeParser<AbstractPropertyValue> p = context.getRegistry().get("output_property");
//            AbstractPropertyValue res = p.parse(node, context);
            try {
                //Yaml yaml = new Yaml();
                String value = ((ScalarPropertyValue) od.getValue()).getValue();
                String name = od.getName();
                //Node node = yaml.loadAs(value, Node.class);
                ParsingResult<ArchiveRoot>  pr = toscaParser.parse(
                        new ByteArrayInputStream(String.format(TOSCA_OUTPUT_TEMPLATE, name, value).getBytes(StandardCharsets.UTF_8)), null);
                OutputDefinition odNew = pr.getResult().getTopology().getOutputs().get(name);
                operation.setOutputDefinition(odNew);//.setValue(odNew);
            } catch (ParsingException e) {
                e.printStackTrace();
            }
//            ObjectMapper mapper = new ObjectMapper();
//            mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
//            try {
//                FunctionPropertyValue function = mapper.readValue(((ScalarPropertyValue) od.getValue()).getValue(),
//                        FunctionPropertyValue.class);
//                if (function != null) {
//                    od.setValue(function);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        outputs.put(operation.getOutputName(), operation.getOutputDefinition());
        topology.setOutputs(outputs);

        log.debug("Add a new output [ {} ] for the topology [ {} ].", operation.getOutputName(), topology.getId());
    }
}
