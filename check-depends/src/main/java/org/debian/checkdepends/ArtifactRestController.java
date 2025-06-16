package org.debian.checkdepends;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST controller to handle artifact search requests via HTTP.
 */
@RestController
@RequestMapping("/api/artifacts")
public class ArtifactRestController {

    private final DbManager databaseService;
    private static final Logger logger = LoggerFactory.getLogger(DbManager.class);

    @Autowired
    public ArtifactRestController(DbManager databaseService) {
        this.databaseService = databaseService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Artifact>> searchArtifacts(
            @RequestParam(value = "groupId", defaultValue = "") String groupId,
            @RequestParam(value = "artifactId", defaultValue = "") String artifactId) {

        logger.debug("REST request - Searching for groupId: " + groupId + ", artifactId: " + artifactId);

        var results = databaseService.searchArtifacts(groupId, artifactId);

        logger.debug("Found " + results.size() + " results.");

        return ResponseEntity.ok(results);
    }
}
