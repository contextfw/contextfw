package net.contextfw.web.application.internal.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.contextfw.web.application.WebApplicationException;
import net.contextfw.web.application.component.Component;
import net.contextfw.web.application.remote.Remoted;

public class ClassScanner extends AbstractScanner {

    public static List<Class<?>> getClasses(String... packageNames) {
        ArrayList<String> list = new ArrayList<String>(packageNames.length);
        Collections.addAll(list, packageNames);
        return getClasses(list);
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages.
     * 
     * @param packageName
     *            The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static List<Class<?>> getClasses(Iterable<String> packageNames) {

        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

        try {
            for (String packageName : packageNames) {
                List<String> resourcePaths = new ArrayList<String>();
                resourcePaths.add(packageName);
                List<ResourceEntry> entries = findResourceEntries(resourcePaths);

                for (ResourceEntry entry : entries) {

                    String fileName = entry.getPath();
                    if (fileName.endsWith(".class") && !fileName.contains("$")) {
                        Class<?> _class;
                        String className = toClassName(fileName);
                        try {
                            _class = Class.forName(className);
                        } catch (ExceptionInInitializerError e) {
                            // happen, for example, in classes, which depend on
                            // Spring to inject some beans, and which fail,
                            // if dependency is not fulfilled
                            _class = Class.forName(className,
                                    false, Thread.currentThread().getContextClassLoader());
                        }
                        classes.add(_class);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new WebApplicationException(e);
        }

        return classes;
    }

    private static String toClassName(String fileName) {
        return fileName.substring(0, fileName.length() - 6).replaceAll("/", "\\.");
    }

    /**
     * Recursive method used to find all classes in a given directory and
     * subdirs.
     * 
     * @param directory
     *            The base directory
     * @param packageName
     *            The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List<Class<?>> findClasses(String parent, File directory, String packageName)
            throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {

            String fileName = file.getName();
            if (file.isDirectory()) {
                assert !fileName.contains(".");
                classes.addAll(findClasses(parent + "/" + fileName, file, packageName + "."
                        + fileName));
            } else if (fileName.endsWith(".class") && !fileName.contains("$")) {
                Class<?> _class;
                try {
                    _class = Class.forName(packageName + '.'
                            + fileName.substring(0, fileName.length() - 6));
                } catch (ExceptionInInitializerError e) {
                    // happen, for example, in classes, which depend on
                    // Spring to inject some beans, and which fail,
                    // if dependency is not fulfilled
                    _class = Class.forName(
                            packageName + '.' + fileName.substring(0, fileName.length() - 6),
                            false, Thread.currentThread().getContextClassLoader());
                }
                classes.add(_class);
            }
        }
        return classes;
    }

    public static List<Class<?>> getParamTypes(Class<?> declaringClass, Method method) {

        Map<String, Class<?>> typeVariables = new HashMap<String, Class<?>>();
        Object parametrizedType = declaringClass.getGenericSuperclass();

        if (parametrizedType instanceof ParameterizedType) {
            TypeVariable<?>[] tv = method.getDeclaringClass().getTypeParameters();
            Type[] paramTypes = ((ParameterizedType) parametrizedType).getActualTypeArguments();

            for (int i = 0; i < tv.length; i++) {
               typeVariables.put(tv[i].getName(), (Class<?>) paramTypes[i]);
            }
        }

        Type[] types = method.getGenericParameterTypes();

        if (types.length == 0) {
            return Collections.emptyList();
        }

        List<Class<?>> rv = new ArrayList<Class<?>>(types.length);

        for (int i = 0; i < types.length; i++) {
            if (types[i] instanceof TypeVariable) {
                rv.add(typeVariables.get(((TypeVariable<?>) types[i]).getName()));
            } else {
                rv.add((Class<?>) types[i]);
            }
            // rv.add(type == null ? types[i] : type);
        }

        return rv;
    }

    public static Method findMethodForName(final Class<? extends Component> cls, String methodName) {
        Class<?> current = cls;
        Method foundMethod = null;
        while (Component.class.isAssignableFrom(current)) {
            for (Method method : current.getDeclaredMethods()) {
                if (foundMethod == null && method.getAnnotation(Remoted.class) == null
                        && method.getName().equals(methodName)) {
                    foundMethod = method;
                } else if (method.getAnnotation(Remoted.class) != null
                        && method.getName().equals(methodName)) {
                    return foundMethod == null ? method : foundMethod;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
