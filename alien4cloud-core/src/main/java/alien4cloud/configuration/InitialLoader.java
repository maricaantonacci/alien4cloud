package alien4cloud.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingResult;
import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.catalog.ArchiveParser;
import org.alien4cloud.tosca.catalog.ArchiveUploadService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.component.repository.exception.CSARUsedInActiveDeployment;
import alien4cloud.component.repository.exception.ToscaTypeAlreadyDefinedInOtherCSAR;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.model.components.CSARSource;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.exception.MissingPlugingDescriptorFileException;
import alien4cloud.plugin.exception.PluginLoadingException;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.context.ToscaContextInjector;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.utils.AlienConstants;
import alien4cloud.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;

/** Load archives and plugins at bootstrap to initialize alien 4 cloud repository. */
@Slf4j
@Component
public class InitialLoader {
    private static final String ARCHIVES_FOLDER = "archives";
    private static final String PLUGINS_FOLDER = "plugins";

    @Inject
    private ArchiveParser parser;
    @Inject
    private ArchiveUploadService csarUploadService;
    @Inject
    private PluginManager pluginManager;
    @Inject
    private ToscaContextInjector toscaContextInjector;

    @Value("${directories.alien_init:}")
    private String alienInitDirectory;
    private Path alienPluginsInitPath;

    @PostConstruct
    public void initialLoad() throws IOException {
        // Ensure tosca context injector is loaded first.
        if (toscaContextInjector == null) {
            log.error("Tosca Context Injector is required.");
        }

        // Components
        if (alienInitDirectory == null || alienInitDirectory.isEmpty()) {
            log.debug("No init directory is configured - skipping initial loading of archives and plugins.");
            return;
        }
        Path alienInitDirectoryPath = Paths.get(alienInitDirectory);
        if (!Files.exists(alienInitDirectoryPath)) {
            log.warn("Specified initial directory [ {} ] cannot be found - skipping initial loading of archives and plugins.", alienInitDirectoryPath.toString());
            return;
        }

        Path alienArchivesInitPath = alienInitDirectoryPath.resolve(ARCHIVES_FOLDER);
        loadArchives(alienArchivesInitPath);

        alienPluginsInitPath = alienInitDirectoryPath.resolve(PLUGINS_FOLDER);
    }

    private void loadArchives(Path rootDirectory) {
        if (!Files.exists(rootDirectory) || !Files.isDirectory(rootDirectory)) {
            log.warn("Skipping archives' initial loading: directory cannot be found {}.", rootDirectory.toString());
            return;
        }
        // this operation is performed using admin role
        SecurityContextImpl adminContext = new SecurityContextImpl();
        Set<SimpleGrantedAuthority> authorities = Sets.newHashSet();
        authorities.add(new SimpleGrantedAuthority(Role.ADMIN.name()));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("alien4cloud_init_loader", "alien4cloud_init_loader", authorities);
        adminContext.setAuthentication(auth);
        SecurityContextHolder.setContext(adminContext);
        // archives must be in zip format and placed in the actual folder
//        try {
//            List<Path> archives = FileUtil.listFiles(rootDirectory, ".+\\.(zip|csar)");
//            Collections.sort(archives);
//            for (Path archive : archives) {
//                try {
//                    log.debug("Initial load of archives from [ {} ].", archive.toString());
//                    csarUploadService.upload(archive, CSARSource.ALIEN, AlienConstants.GLOBAL_WORKSPACE_ID);
//                } catch (AlreadyExistException e) {
//                    log.debug("Skipping initial upload of archive [ {} ]. Archive has already been loaded.", archive.toString(), e);
//                } catch (CSARUsedInActiveDeployment e) {
//                    log.debug("Skipping initial upload of archive [ {} ]. Archive is used in an active depoyment, and then cannot be overrided.",
//                            archive.toString(), e);
//                } catch (ParsingException e) {
//                    log.error("Initial upload of archive [ {} ] has failed.", archive.toString(), e);
//                } catch (ToscaTypeAlreadyDefinedInOtherCSAR e) {
//                    log.debug("Skipping initial upload of archive [ {} ], it's archive contain's a tosca type already defined in an other archive."
//                            + e.getMessage(), archive.toString(), e);
//                }
//            }
//        } catch (IOException e) {
//            log.error("Failed to load initial archives", e);
//        } finally {
//            // clear the security context
//            SecurityContextHolder.clearContext();
//        }

        try {
            //SortedMap<Path, ParsingResult<ArchiveRoot>> parsedCSARs = orderFiles(rootDirectory);
            List<Path> archives = orderFiles(rootDirectory);
            for (Path archive : archives) {
                try {
                    log.debug("Initial load of archives from [ {} ].", archive.toString());
                    csarUploadService.upload(archive, CSARSource.ALIEN, AlienConstants.GLOBAL_WORKSPACE_ID);
                }  catch (ToscaTypeAlreadyDefinedInOtherCSAR e) {
                    log.debug("Skipping initial upload of archive [ {} ], it's archive contain's a tosca type already defined in an other archive."
                            + e.getMessage(), archive.toString(), e);
                } catch (ParsingException e) {
                    log.error("Initial upload of archive [ {} ] has failed.", archive.toString(), e);
                } catch (CSARUsedInActiveDeployment e) {
                                        log.debug("Skipping initial upload of archive [ {} ]. Archive is used in an active depoyment, and then cannot be overrided.",
                                                archive.toString(), e);
                }

            }
        } finally {
            // clear the security context
            SecurityContextHolder.clearContext();
        }
    }

    protected List<Path> orderFiles(Path rootDirectory) {

        List<Path> result = new ArrayList<>();
        Map<String, ArchiveNode> parsedCSARs = new TreeMap<>();
        try {
            List<Path> archives = FileUtil.listFiles(rootDirectory, ".+\\.(zip|csar)");
            // TODO: remove this sort after testing the algo
            Collections.sort(archives);
            for (Path archive : archives) {
                try {
                    ParsingResult<ArchiveRoot> parsingResult = parser.parse(archive, AlienConstants.GLOBAL_WORKSPACE_ID);
                    ArchiveRoot ar = parsingResult.getResult();
                    Set<CSARDependency> dependencies = ar.getArchive().getMissingDependencies();
                    if (ar.getArchive().getDependencies() != null)
                        dependencies.addAll(ar.getArchive().getDependencies());
                    ArchiveNode an = new ArchiveNode(ar, archive, dependencies);
                    parsedCSARs.put(ArchiveNode.generateIdAR(ar), an);
                } catch (ParsingException e) {
                    log.error("Initial upload of archive [ {} ] has failed.", archive.toString(), e);
                }

            }

            // Create relationships graph
            for (Map.Entry<String, ArchiveNode> entry: parsedCSARs.entrySet()) {
                ArchiveNode an = entry.getValue();
                Set<CSARDependency> dependencies = an.getDependencies();
                if (dependencies != null) {
                    for (CSARDependency cd: dependencies) {
                        String id = ArchiveNode.generateIdCD(cd);
                        if (parsedCSARs.containsKey(id)) {
                            ArchiveNode dependency = parsedCSARs.get(id);
                            dependency.addImportedBy(an);
                        } else {
                            log.error("Import dependency named " + cd.getName() + " and version " + cd.getVersion()
                                    + " of CSAR " + an.getArchiveRoot().getArchive().getName() + " version " + an.getArchiveRoot().getArchive().getVersion()
                                    + " cannot be found in the list of init CSARs");
                        }
                    }
                }
            }

            final Queue<ArchiveNode> nodesLefttoProcess = parsedCSARs.entrySet().stream().filter(an -> an.getValue().getDependencies().isEmpty())
                    .map(entry -> entry.getValue())
                    .collect(Collectors.toCollection(ArrayDeque::new));
            while (!nodesLefttoProcess.isEmpty()) {
                ArchiveNode an = nodesLefttoProcess.poll();
                if (!an.isVisited())
                    result.add(an.getPath());
                an.setVisited(true);
                for (ArchiveNode importedBy: an.getImportedBy().values()) {
                    if (!importedBy.isVisited())
                        nodesLefttoProcess.add(importedBy);
                }
            }
        } catch (IOException e) {
            log.error("Failed to load initial archives", e);
        }


        return result;
    }


    /**
     * Load plugins from the initialization plugins folder.
     */
    public void loadPlugins() {
        if (alienInitDirectory == null || alienInitDirectory.isEmpty() || alienPluginsInitPath == null) {
            log.debug("No init directory is configured - skipping initial loading of plugins.");
            return;
        }

        if (!Files.exists(alienPluginsInitPath) || !Files.isDirectory(alienPluginsInitPath)) {
            log.warn("Skipping plugins' initial loading: directory cannot be found {}.", alienPluginsInitPath.toString());
            return;
        }

        try {
            List<Path> plugins = FileUtil.listFiles(alienPluginsInitPath, ".+\\.(zip)");
            pluginManager.uploadInitPlugins(plugins);
        } catch (IOException e) {
            log.error("Failed to load initial plugins", e);
        }
    }

    @Getter
    protected static class ArchiveNode {
        protected ArchiveRoot archiveRoot;
        protected Path path;
        protected Map<String, ArchiveNode> importedBy;
        protected Set<CSARDependency> dependencies;
        @Setter
        protected boolean visited;


        public ArchiveNode(ArchiveRoot archiveRoot, Path path, Set<CSARDependency> dependencies) {
            this.archiveRoot = archiveRoot;
            this.path = path;
            this.dependencies = dependencies;
            importedBy = new HashMap<>();
            visited = false;
        }

        public String getId() {
            return generateIdAR(this.archiveRoot);
        }

        public void addImportedBy(ArchiveNode node) {
            importedBy.put(generateIdAR(node.getArchiveRoot()), node);
        }

        public static String generateIdAR(ArchiveRoot ar) {
            return ar.getArchive().getName() + ": " +  ar.getArchive().getVersion();
        }

        public static String generateIdCD(CSARDependency cd) {
            return cd.getName() + ": " +  cd.getVersion();
        }
    }
}
