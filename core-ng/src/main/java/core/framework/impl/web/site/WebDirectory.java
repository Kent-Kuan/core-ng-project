package core.framework.impl.web.site;

import core.framework.api.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author neo
 */
public class WebDirectory {
    private final Logger logger = LoggerFactory.getLogger(WebDirectory.class);

    private final Path root;
    public boolean localEnv;

    public WebDirectory() {
        this.root = locateRootDirectory();
    }

    private Path locateRootDirectory() {
        String value = System.getProperty("core.web");
        if (value != null) {
            Path path = Paths.get(value).toAbsolutePath();
            if (Files.exists(path) && Files.isDirectory(path)) {
                logger.info("found -Dcore.web, use it as web directory, path={}", path);
                return path;
            }
        } else {
            Path path = Paths.get("./src/main/dist/web").toAbsolutePath();
            if (Files.exists(path) && Files.isDirectory(path)) {
                logger.warn("found local web directory, this should only happen in local dev env or test, path={}", path);
                localEnv = true;
                return path;
            }
        }
        logger.info("can not locate web directory");
        return null;
    }

    public Path path(String path) {
        if (path.charAt(0) != '/') throw Exceptions.error("path must start with '/', path={}", path);
        return root().resolve(path.substring(1)).toAbsolutePath();
    }

    public Path root() {
        if (root == null)
            throw new Error("can not find web path, check -Dcore.web or set working dir to be module path for local dev env.");
        return root;
    }
}