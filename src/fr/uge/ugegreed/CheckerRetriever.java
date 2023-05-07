package fr.uge.ugegreed;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;


public final class CheckerRetriever {
    private static final Logger logger = Logger.getLogger(CheckerRetriever.class.getName());
    private static final Map<URL, Checker> fileCache = new HashMap<>();

    /**
     * This method downloads the jar file from the given url
     * and creates an instance of the class assuming it implements the
     * fr.uge.ugegreed.Checker interface.
     * This method can both be used retrieve the class from a local jar file
     * or from a jar file provided by an HTTP server. The behavior depends
     * on the url parameter.
     * @param url the url of the jar file
     * @param className the fully qualified name of the class to load
     * @return an instance of the class if it exists
     */
    public static Optional<Checker> retrieveCheckerFromURL(URL url, String className, boolean useCache) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(className);
        if (useCache) {
            if (fileCache.containsKey(url)) {
                logger.info("Using Cached File for " + url);
                return Optional.of(fileCache.get(url));
            }
            logger.info("No Cached file found for " + url +", downloading...");
        }

        var urls = new URL[]{url};
        var urlClassLoader = new URLClassLoader(urls,Thread.currentThread().getContextClassLoader());
        try {
            var clazz = Class.forName(className, true, urlClassLoader);
            var constructor = clazz.getDeclaredConstructor();
            var instance = constructor.newInstance();
            Checker result = (Checker) instance;
            fileCache.put(url, result);
            return Optional.of(result);
        } catch (ClassNotFoundException e) {
            logger.info("The class %s was not found in %s. The jarfile might not be present at the given URL.".formatted(className, url));
            return Optional.empty();
        } catch (NoSuchMethodException e) {
            logger.info("Class %s in jar %s cannot be cast to fr.uge.ugegreed.Checker".formatted(className, url));
            return Optional.empty();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            logger.info("Failed to create an instance of %s".formatted(className));
            return Optional.empty();
        }
    }

    public static Optional<Checker> checkerFromDisk(Path jarPath, String className, boolean useCache) {
        try {
            var url = jarPath.toUri().toURL();
            return retrieveCheckerFromURL(url, className, useCache);
        } catch (MalformedURLException e) {
            logger.info("URL is malformed");
            return Optional.empty();
        }
    }

}
