package alien4cloud.model.components;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

/**
 * Defines a dependency on a CloudServiceArchive.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of={"name", "version", "toscaDefinitionDependency"})
@ToString
public class CSARDependency {
    @NonNull
    @StringField(indexType = IndexType.not_analyzed)
    private String name;

    @NonNull
    @StringField(indexType = IndexType.not_analyzed)
    private String version;
    
    private String file;
    
    private String repository;
    
    private boolean toscaDefinitionDependency;
    
    public CSARDependency(String name) {
    	this(name, Csar.DEFAULT_CSAR_VERSION);
    }
    
    public CSARDependency(String name, String version) {
    	this.name = name;
    	this.version = version;
    }
}
