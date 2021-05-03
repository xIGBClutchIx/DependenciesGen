package me.clutchy.dependenciesgen.downloader;

import sun.misc.Unsafe;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoadJarsUtil {

    private static boolean urlCheckDisabled = false;

    // Add our library to our url class loader.
    public static void addFile(ClassLoader classLoader, Logger logger, File file) {
        if (!urlCheckDisabled) {
            urlCheckDisabled = true;
            disableWarning();
        }
        if (classLoader instanceof URLClassLoader) {
            Class<URLClassLoader> classLoaderClass = URLClassLoader.class;
            try {
                Method method = classLoaderClass.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classLoader, file.toURI().toURL());
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | MalformedURLException e) {
                logger.log(Level.SEVERE, "Error loading library: " + file.getName(), e);
                System.exit(0);
            }
        } else {
            logger.severe("Classloader not instance of URLClassLoader");
            System.exit(0);
        }
    }

    // Remove addURL warning
    private static void disableWarning() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);
            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception ignore) {
        }
    }
}
