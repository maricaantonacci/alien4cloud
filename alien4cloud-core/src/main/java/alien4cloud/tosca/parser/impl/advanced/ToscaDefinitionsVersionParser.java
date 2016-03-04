package alien4cloud.tosca.parser.impl.advanced;

import com.google.common.collect.Sets;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import java.util.Set;

import javax.annotation.Resource;

import alien4cloud.csar.services.CsarService;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.model.ArchiveRoot;

@Component
public class ToscaDefinitionsVersionParser extends ScalarParser {
    
    @Resource
    private CsarService csarService;

    @Override
    public String parse(Node node, ParsingContextExecution context) {
        String toscaDefinitionsVersion = super.parse(node, context);
        
        CSARDependency dependency = new CSARDependency(toscaDefinitionsVersion);
        dependency.setToscaDefinitionDependency(true);
        if (csarService.getIfExists(dependency.getName(), dependency.getVersion()) != null) {
            Csar archive = ((ArchiveRoot) context.getParent()).getArchive();
            Set<CSARDependency> dependencies = archive.getDependencies();
            if (dependencies == null) {
                dependencies = Sets.newLinkedHashSet();
            } 
            dependencies.add(dependency);
            archive.setDependencies(dependencies);
        }
        return toscaDefinitionsVersion;
    }

}
