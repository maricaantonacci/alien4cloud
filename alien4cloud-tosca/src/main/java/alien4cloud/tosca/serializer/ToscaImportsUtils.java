package alien4cloud.tosca.serializer;

import java.util.Set;

import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.CSARDependencyWithUrl;
import org.alien4cloud.tosca.normative.ToscaNormativeImports;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * A {@code ToscaImportsUtils} is a helper class that generates TOSCA imports.
 *
 * @author Loic Albertin
 */
public class ToscaImportsUtils {

    public static String generateImports(Set<CSARDependency> dependencies) {
        StringBuilder sb = new StringBuilder();
        safe(dependencies)
            .stream()
            .filter(d -> !ToscaNormativeImports.TOSCA_NORMATIVE_TYPES.equals(d.getName()))
            .forEach(d -> {
            if (sb.length() != 0) {
                sb.append("\n");
            }
            sb.append("  - ");
            sb.append(d.getName());
            if (d instanceof CSARDependencyWithUrl) {
                sb.append(": ");
                sb.append(((CSARDependencyWithUrl) d).getUrl());
            } else {
                sb.append(":");
                sb.append(d.getVersion());
            }
        });
        return sb.toString();
    }
}
