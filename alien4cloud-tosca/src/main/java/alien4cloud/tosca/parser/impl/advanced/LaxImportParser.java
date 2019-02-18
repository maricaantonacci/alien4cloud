package alien4cloud.tosca.parser.impl.advanced;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.InvalidVersionException;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.hibernate.validator.internal.constraintvalidators.URLValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import lombok.extern.slf4j.Slf4j;

/**
 * Import parser that doesn't validate anything
 * For validation of version or presence in catalog, see {@link ImportParser}
 */
@Slf4j
@Component
public class LaxImportParser implements INodeParser<CSARDependency> {
    @Resource
    private ScalarParser scalarParser;
    
    @Resource
    private BaseParserFactory baseParserFactory;
    
    
    @Override
    public CSARDependency parse(Node node, ParsingContextExecution context) {
      MapParser<String> mapParser = baseParserFactory.<String>getMapParser(scalarParser, "string");
        Map<String, String> value = mapParser.parse(node, context);
        if (!value.isEmpty()) {
          Entry<String, String> entry = value.entrySet().iterator().next();
          String dependencyName = entry.getKey();
          String dependencyVersion = entry.getValue();
          // Check if the provided version is actually a URL
          // TODO (DEEP): THIS APPROACH GETS ONE OF THE EXISTING VERSIONS IN A4C! NOT THE LATEST 
          if (UrlValidator.getInstance().isValid(dependencyVersion)) {
            
            List<Csar> csars = ToscaContext.get().getCsarsByName(dependencyName, 100);//csarService.getFirst(dependencyName);
            if (!csars.isEmpty()) {
              log.info(String.format("import URL detected, replace TOSCA http path %s with version %s", dependencyVersion, 
                  csars.get(0).getVersion()));
              dependencyVersion = csars.get(0).getVersion();
            } else {
              context.getParsingErrors()
              .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR,
                      "Name of the CSAR mentioned in the import cannot be found in the Alien4Cloud DB.", node.getStartMark(),
                      "Please upload at least one CSAR with the name specified in the import, or modify the name of the import", 
                      node.getEndMark(), "Import"));
              return null;
            }
          } else {
            // check that version has the right format
            try {
                VersionUtil.parseVersion(dependencyVersion);
            } catch (InvalidVersionException e) {
                context.getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR,
                                "Version specified in the dependency is not a valid version.", node.getStartMark(),
                                "Dependency should be specified as name:version", node.getEndMark(), "Import"));
                return null;
            }
          }
          return new CSARDependency(dependencyName, dependencyVersion);
            
        }
        return null;
    }

//    @Override
//    public CSARDependency parse(Node node, ParsingContextExecution context) {
//        String valueAsString = scalarParser.parse(node, context);
//        if (StringUtils.isNotBlank(valueAsString)) {
//            if (valueAsString.contains(":")) {
//                String[] dependencyStrs = valueAsString.split(":");
//                if (dependencyStrs.length == 2) {
//                    // Eliminate unwanted chars
//                    String dependencyName = dependencyStrs[0]
//                        .trim()
//                        .replaceAll("^\"|^\'|\"$|\'$", "");
//                    String dependencyVersion = dependencyStrs[1]
//                        .trim()
//                        .replaceAll("^\"|^\'|\"$|\'$", "");
//                    // check that version has the righ format
//                    try {
//                        VersionUtil.parseVersion(dependencyVersion);
//                    } catch (InvalidVersionException e) {
//                        context.getParsingErrors()
//                                .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR,
//                                        "Version specified in the dependency is not a valid version.", node.getStartMark(),
//                                        "Dependency should be specified as name:version", node.getEndMark(), "Import"));
//                        return null;
//                    }
//                    return new CSARDependency(dependencyName, dependencyVersion);
//                }
//                context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR, "Import definition is not valid",
//                        node.getStartMark(), "Dependency should be specified as name:version", node.getEndMark(), "Import"));
//            } else {
//                context.getParsingErrors()
//                        .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR, "Relative import is currently not supported in Alien 4 Cloud",
//                                node.getStartMark(), "Dependency should be specified as name:version", node.getEndMark(), "Import"));
//            }
//        }
//        return null;
//    }
}