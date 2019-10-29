package alien4cloud.tosca.parser;

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.InputStream;

@Component
public class ToscaSimpleParser extends ToscaParser {

    public ParsingResult<ArchiveRoot> parse(InputStream yamlStream, ArchiveRoot instance) throws ParsingException {
        StreamReader sreader = new StreamReader(new UnicodeReader(yamlStream));
        Composer composer = new Composer(new ParserImpl(sreader), new Resolver());
        Node rootNode = null;
        try {
            rootNode = composer.getSingleNode();
            if (rootNode == null) {
                throw new ParsingException("none", new ParsingError(ErrorCode.SYNTAX_ERROR, "Empty file.", new Mark("root", 0, 0, 0, null, 0),
                        "No yaml content found in file.", new Mark("root", 0, 0, 0, null, 0), "none"));
            }
        } catch (MarkedYAMLException exception) {
            throw new ParsingException("none", new ParsingError(ErrorCode.INVALID_YAML, exception));
        }

        try {
            return doParsing(rootNode, instance);
        } catch (ParsingException e) {
            throw e;
        }
    }

    private ParsingResult<ArchiveRoot> doParsing(Node rootNode, ArchiveRoot instance) throws ParsingException {
        boolean createContext = !ParsingContextExecution.exist();
        try {
            if (createContext) { // parser can reuse an existing context if provided.
                ParsingContextExecution.init();
            }
            //ParsingContextExecution.setFileName(fileName);

            ParsingContextExecution fake = new ParsingContextExecution();

            INodeParser<ArchiveRoot> nodeParser = getParser(rootNode, fake);

            ArchiveRoot parsedObject;
            if (nodeParser instanceof TypeNodeParser) {
                parsedObject = ((TypeNodeParser<ArchiveRoot>) nodeParser).parse(rootNode, fake, instance);
            } else {
                // let's start the parsing using the version related parsers
                parsedObject = nodeParser.parse(rootNode, fake);
            }


            return new ParsingResult<ArchiveRoot>(parsedObject, ParsingContextExecution.getParsingContext());

        } finally {
            if (createContext) {
                ParsingContextExecution.destroy();
            }
        }
    }

}
