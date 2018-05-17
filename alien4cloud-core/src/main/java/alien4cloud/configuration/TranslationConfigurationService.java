package alien4cloud.configuration;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * This service read the a4c configuration to determine the supported languages
 */
@Service
public class TranslationConfigurationService {

    @Value("${languages.prefix:locale}")
    private String prefixLanguageFromConfig;

    @SneakyThrows
    /**
     * List the supported languages with the prefix set in config from the path.
     */
    public Set<String> getSupportedLanguages() {
        Set supportedLanguages = Sets.newHashSet();
        ClassPathResource pathToLanguages = new ClassPathResource("data/languages");
        File folder = pathToLanguages.getFile();
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(prefixLanguageFromConfig)) {
                supportedLanguages.add(listOfFiles[i].getName().split(prefixLanguageFromConfig)[1].split("\\.")[0].substring(1));
            }
        }
        return supportedLanguages;
    }

    /**
     * Associate the name of a language to the code
     */
    public Map<String, String> getNameOfLanguagesCode() {
        Set<String> languagesCode = getSupportedLanguages();
        Map<String, String> fullName = Maps.newHashMap();
        for (String code : languagesCode) {
            Locale locale = new Locale(code.split("-")[0], code.split("-")[1], "");
            fullName.put(code, locale.getDisplayLanguage(locale));
        }
        return fullName;
    }
}
