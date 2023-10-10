package io.mvnpm.newfile;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.file.FileStoreEvent;
import io.mvnpm.mavencentral.sync.ContinuousSyncService;
import io.mvnpm.npm.model.Name;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;

/**
 * Once a new jar file is created, we need to kick of the sync process
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class AutoSyncService {

    @Inject
    ContinuousSyncService projectUpdater;

    private final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.jar");

    @ConsumeEvent("new-file-created")
    @Blocking
    public void newFileCreated(FileStoreEvent fse) {
        if (matcher.matches(fse.filePath().getFileName())) {
            Name name = fse.name();
            String version = fse.version();
            boolean queued = projectUpdater.initializeSync(name, version);
            if (queued) {
                Log.info(name.displayName() + " " + version + " added to the sync queue");
            }
        }
    }

}
