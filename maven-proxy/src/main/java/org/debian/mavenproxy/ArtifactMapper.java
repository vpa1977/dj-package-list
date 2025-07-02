package org.debian.mavenproxy;

import org.debian.maven.repo.Dependency;
import org.debian.maven.repo.DependencyRuleSet;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

/**
 * Translates requested artifact to Debian coordiantes
 */
public class ArtifactMapper {
    private final boolean map;
    private final DbManager dbManager;
    private final DependencyRuleSet ignoreRuleSet;
    private final DependencyRuleSet replaceRuleSet;
    private final HashMap<ArtifactParseUtil.Artifact, File> mappedFiles = new HashMap<>();

    public ArtifactMapper(boolean map, DbManager dbManager, List<String> replaceRules, List<String> ignoreRules) throws IOException {
        this.map = map;
        this.dbManager = dbManager;
        this.ignoreRuleSet = RuleParser.parseRules(ignoreRules, "ignore");
        this.replaceRuleSet = RuleParser.parseRules(replaceRules, "replace");
    }

    /**
     * This method finds the best version to use in Debian Repo.
     * It returns RemappedArtifact with:
     *  - Artifact - original requested coordinates
     *  - oldRequestPath - original request path
     *  - newRequestPath - path to the artifact in maven repo
     * @param path - original request path
     * @return RemappedArtifact
     */
    public RemappedArtifact mapRequestPath(Path debianPath, String path) {
        ArtifactParseUtil.Artifact art = ArtifactParseUtil.parse(path);
        if (!map) {
            return new RemappedArtifact(art, path, path);
        }

         try {
            String filePath = path.substring(path.lastIndexOf("/")+1);
            String ext = filePath.substring(filePath.lastIndexOf(".")+1);

            if (!ignoreRuleSet.findMatchingRules(new Dependency(art.groupId(), art.name(), art.type(), art.version())).isEmpty()){
                return new RemappedArtifact(art, path, "placeholder/gradle-dependency-placeholder-1.0."+ ext);
            }

            List<String> versions = dbManager.findVersion(art.groupId(), art.name());
            for (var version : versions) {
                String newFileName = art.name() + "-"+ version + "." + ext;
                var testPath = art.groupId().replace(".", "/") + "/" + art.name() + "/" +  version + "/" + newFileName;
                if (debianPath.resolve(testPath).toFile().exists()) {
                    return new RemappedArtifact(art, path, testPath);
                }
            }

            return new RemappedArtifact(art, path, path);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method is used to patch POM files returned to the caller
     * It replaces pom with the patched pom. NB. SHA1 files is not handled yet.
     * @param art - original requested artifact
     * @param sourceFile - source file in maven repo
     * @return file to return to the caller
     */
    public File mapFile(ArtifactParseUtil.Artifact art, File sourceFile)  {
        if (!map) {
            return sourceFile;
        }
        if (!"pom".equals(art.type())) {
            return sourceFile;
        }
        File mapped = mappedFiles.get(art);
        if (mapped != null) {
            return mapped;
        }
        try {
            File newFile = Files.createTempFile("temp", ".pom").toFile();
            newFile.deleteOnExit();
            POMMapper.mapPom(sourceFile, newFile, art.groupId(), art.name(), art.version());
            mappedFiles.put(art, newFile);
            return newFile;
        } catch (ParserConfigurationException | TransformerException | SAXException | IOException e) {
            throw new RuntimeException("Mapping failed " + e.getMessage());
        }
    }

}
