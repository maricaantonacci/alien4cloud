package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.elasticsearch.common.collect.Sets;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.util.Collection;
import java.util.Set;

import alien4cloud.csar.services.CsarService;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ReferencedParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.impl.base.SetParser;

@Component
public class ImportsParser extends SetParser<CSARDependency> {
    public ImportsParser() {
        super(new ReferencedParser<CSARDependency>("import_definition"), "Imports");
    }

    @Resource
    private CsarService csarService;
    @Resource
    private ScalarParser scalarParser;

    @Override
    public Set<CSARDependency> parse(Node node, ParsingContextExecution context) {
        ArchiveRoot archiveRoot = (ArchiveRoot) context.getParent();
        Set<CSARDependency> dependencies = archiveRoot.getArchive().getDependencies();
        if (node instanceof SequenceNode) {
            Collection<CSARDependency> parsedDependencies = super.parse(node, context);
            if (parsedDependencies != null) {       
                if (dependencies == null) {
                    dependencies = Sets.newLinkedHashSet(parsedDependencies);
                } else {
                    for (CSARDependency parsedDependency : parsedDependencies) {
                        if (!dependencies.contains(parsedDependency)) {
                            dependencies.add(parsedDependency);
                        }
                    }
                }
            }
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Imports");
        }
        return dependencies;
    }
}