package org.alien4cloud.tosca.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Defines a dependency on a CloudServiceArchive with import URL.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CSARDependencyWithUrl extends CSARDependency {

    @NonNull
    private String url;

    public CSARDependencyWithUrl(String name, String version, String url) {
        super(name, version);
        this.url = url;
    }

    public CSARDependencyWithUrl(String name, String version, String hash, String url) {
        super(name, version, hash);
        this.url = url;
    }
}