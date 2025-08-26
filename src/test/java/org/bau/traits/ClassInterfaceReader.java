package org.bau.traits;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ClassInterfaceReader {

    public static void main(String[] args) throws ClassNotFoundException {
        TreeMap<String, TypeOrTrait> classMap = new TreeMap<>();

        // Start with seed classes and recursively discover all referenced classes
        Set<String> discoveredClasses = init("org.bau");
        discoveredClasses.addAll(init("org.bau.parser"));
        discoveredClasses.addAll(init("org.bau.runtime"));
        discoveredClasses.addAll(init("org.bau.tools"));

        addAllFromJar(discoveredClasses, "/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home/lib/src.zip");

        Set<String> processedClasses = new HashSet<>();

        int[] slotIdUsage = new int[100];

        System.out
                .println("Starting recursive class discovery with " + discoveredClasses.size() + " initial classes...");

        while (!discoveredClasses.isEmpty()) {
            String className = discoveredClasses.iterator().next();
            discoveredClasses.remove(className);
            if (processedClasses.contains(className)) {
                continue;
            }
            processedClasses.add(className);
//            System.out.println("Processing class: " + className);

            try {
                Class<?> clazz = Class.forName(className);
                boolean isInterface = clazz.isInterface();
                List<String> methods = new ArrayList<>();
                List<String> superInterfaces = new ArrayList<>();
                List<String> implementedInterfaceMethods = new ArrayList<>();

                // Discover referenced classes from this class
                Set<String> referencedClasses = getReferencedClasses(clazz);
                discoveredClasses.addAll(referencedClasses);
//                for (String refClass : referencedClasses) {
//                    if (!processedClasses.contains(refClass) && !refClass.isEmpty()) {
//                        discoveredClasses.add(refClass);
//                    }
//                }

                if (isInterface) {
                    for (Method method : clazz.getDeclaredMethods()) {
                        if (Modifier.isPublic(method.getModifiers())) {
                            methods.add(getMethodSignature(method));
                        }
                        implementedInterfaceMethods.add(method.getName());
                    }
                    for (Class<?> superInterface : clazz.getInterfaces()) {
                        discoveredClasses.add(superInterface.getName());
                        superInterfaces.add(superInterface.getName());
                    }
                } else {
                    Set<String> interfaceMethods = new HashSet<>();
                    for (Class<?> implementedInterface : clazz.getInterfaces()) {
                        superInterfaces.add(implementedInterface.getName());
                        discoveredClasses.add(implementedInterface.getName());
                        for (Method interfaceMethod : implementedInterface.getMethods()) {
                            interfaceMethods.add(getMethodSignature(interfaceMethod));
                        }
                    }
                    for (Method method : clazz.getDeclaredMethods()) {
                        if (Modifier.isPublic(method.getModifiers())) {
                            String methodSig = getMethodSignature(method);
                            if (interfaceMethods.contains(methodSig)) {
                                implementedInterfaceMethods.add(methodSig);
                            }
                        }
                    }
                }
                TypeOrTrait classInfo = new TypeOrTrait(clazz, className, isInterface, methods, superInterfaces,
                        implementedInterfaceMethods);
                classMap.put(className, classInfo);
            } catch (ExceptionInInitializerError e) {
//                System.out.println("Warning: Could not load class " + className);
            } catch (ClassNotFoundException e) {
//                System.out.println("Warning: Could not find class " + className);
            } catch (Exception e) {
                // Skip classes that can't be processed
//                System.out.println("Warning: Could not process class " + className + ": " + e.getMessage());
            }
        }

        // Print results
        System.out.println("\n" + "=".repeat(60));
        System.out.println("FINAL RESULTS");
        System.out.println("=".repeat(60));
        System.out.println("Processed " + processedClasses.size() + " classes");
        System.out.println("Found " + classMap.size() + " classes and interfaces");
        System.out.println("=".repeat(60));

        TypeList typeList = new TypeList();

        for (Map.Entry<String, TypeOrTrait> entry : classMap.entrySet()) {
            String name = entry.getKey();
            TypeOrTrait ci = entry.getValue();
            if (ci.isInterface) {
                ArrayList<TraitFunction> fl = new ArrayList<>();
                for (String m : ci.implementedInterfaceMethods) {
                    fl.add(new TraitFunction(fl.size(), m, null));
                }
                Trait t = new Trait(typeList.traits.size(), name, fl);
                typeList.traits.put(name, t);
                for (TraitFunction f : fl) {
                    f.trait = t;
                }
            }
        }
        // add inherited interfaces
        for (Map.Entry<String, TypeOrTrait> entry : classMap.entrySet()) {
            String name = entry.getKey();
            TypeOrTrait ci = entry.getValue();
            if (ci.isInterface) {
                Trait t = typeList.traits.get(name);
                for (String s : ci.superInterfaces) {
                    Trait st = typeList.traits.get(s);
                    t.inherited.add(st);
                }
            }
        }

        for (Map.Entry<String, TypeOrTrait> entry : classMap.entrySet()) {
            String name = entry.getKey();
            TypeOrTrait ci = entry.getValue();
            if (!ci.isInterface) {
                if (ci.getImplementedInterfaces() <= 0) {
                    continue;
                }
                Type t = new Type();
                t.name = name;
                for (String s : ci.superInterfaces) {
                    Trait trait = typeList.traits.get(s);
                    if (trait == null) {
                        System.out.println("processes?" + processedClasses.contains(s) + " " + s);
                        System.out.println("??");
                    }
                    trait.usedIn.add(t);
                    t.traits.add(trait);
                    t.traitFunctions.addAll(trait.functionList);
                }
                typeList.types.put(t.name, t);
                // System.out.println("t: " + t);
            }
        }
        int maxSlot = 0;
        ArrayList<Trait> tlist = new ArrayList<>();
        for (Trait trait : typeList.traits.values()) {
            tlist.add(trait);
        }
        tlist.sort(new Comparator<Trait>() {
            @Override
            public int compare(Trait o1, Trait o2) {
                return -Integer.compare(o1.usedIn.size(), o2.usedIn.size());
            }
        });
        for (Trait trait : tlist) {
            if (trait.functionList.size() == 0) {
                // traits without functions (marker)
                continue;
            }
            if (trait.usedIn.size() == 0) {
                // unused trait
                continue;
            }

            // we try to find the lowest slot this trait can use
            // ----------------------
            BitSet usedSlots = new BitSet();
            for (Type t : trait.usedIn) {
                for (Trait t2 : t.traits) {
                    if (t2.slot < 0) {
                        continue;
                    }
                    if (t2 == trait) {
                        continue;
                    }
                    usedSlots.set(t2.slot);
                }
            }
            int i = 0;
            while (usedSlots.get(i)) {
                i++;
            }
            if (i > maxSlot) {
                System.out.println("maxSlot: " + maxSlot);
                maxSlot = i;
            }
            trait.slot = i;
            slotIdUsage[i]++;
            // ----------------------

            if (trait.name.startsWith("java.util.")) {
                System.out.println("usedIn=" + trait.usedIn.size() + ": " + trait);
            }
        }
        int usedSlotCount = 0, freeSlotCount = 0;
        int maxTc = 0;
        for (Type t : typeList.types.values()) {
            HashSet<String> nonMarkerTraits = new HashSet<String>();
            t.collectAllNonMarkerTraits(nonMarkerTraits);
            if (nonMarkerTraits.size() > maxTc) {
                maxTc = nonMarkerTraits.size();
                System.out.println("RECORD " + t.name + " " + nonMarkerTraits);
            }
//            System.out.println("type " + t.name + " tc: " + nonMarkerTraits.size());
            BitSet usedSlots = new BitSet();
            int maxSlotId = 0;
            int slotsUsed = 0;
            for (Trait trait : t.traits) {
                if (trait.functionList.size() == 0) {
                    continue;
                }
                slotsUsed++;
//                System.out.println("    trait slot=" + trait.slot + " name=" + trait.name + " functions=" + trait.functionList.size());
                if (trait.slot < 0) {
                    throw new IllegalStateException();
                }
                if (trait.slot > maxSlotId) {
                    maxSlotId = trait.slot;
                }
                if (usedSlots.get(trait.slot)) {
                    throw new IllegalStateException();
                }
                usedSlots.set(trait.slot);
            }
            usedSlotCount += slotsUsed;
            freeSlotCount += maxSlotId + 1 - slotsUsed;
        }
        System.out.println("max non-marker trait count: " + maxTc);
        System.out.println("used slots = " + usedSlotCount);
        System.out.println("free slots = " + freeSlotCount);
        for (int i = 0; i < slotIdUsage.length; i++) {
            if (slotIdUsage[i] != 0) {
                System.out.println("slot " + i + ": " + slotIdUsage[i] + " traits");
            }
        }
    }

    private static void addAllFromJar(Set<String> target, String jarFileName) {
        try {
            Iterator<String> classIterator = JarReader.getClassFileIterator(jarFileName);
            while (classIterator.hasNext()) {
                String className = classIterator.next();
                if (className.startsWith("jdk.")) {
                    continue;
                }
                if (className.startsWith("sun.")) {
                    continue;
                }
                target.add(className);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<String> init(String packageName) {
        Set<String> discoveredClasses = new HashSet<>();

        // Initialize discoveredClasses by scanning the specified package
        System.out.println("Initializing with " + packageName + " packages...");
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String resourcePath = packageName.replace('.', '/');
            URL resource = classLoader.getResource(resourcePath);
            if (resource != null && resource.getProtocol().equals("file")) {
                File directory = new File(resource.getFile());
                if (directory.exists()) {
                    List<Class<?>> packageClasses = findClasses(directory, packageName);
                    System.out.println("Found " + packageClasses.size() + " classes in " + packageName + " packages");

                    for (Class<?> clazz : packageClasses) {
                        discoveredClasses.add(clazz.getName());
                    }
                } else {
                    System.out.println(packageName + " directory not found, using fallback seed classes");
                }
            } else {
                System.out.println(packageName + " resource not found, using fallback seed classes");
            }
        } catch (Exception e) {
            System.out.println("Error scanning " + packageName + " packages: " + e.getMessage());
        }

        return discoveredClasses;
    }

    private static Set<String> getReferencedClasses(Class<?> clazz) {

        Set<String> classes = new HashSet<>();

        for (Class<?> c : clazz.getDeclaredClasses()) {
            classes.add(c.getName());
        }
        for (Class<?> c : clazz.getClasses()) {
            classes.add(c.getName());
        }

        try {
            // Superclass
            if (clazz.getSuperclass() != null) {
                classes.add(clazz.getSuperclass().getName());
            }

            // Implemented interfaces
            for (Class<?> iface : clazz.getInterfaces()) {
                classes.add(iface.getName());
            }

            // Method parameter and return types
            for (Method method : clazz.getDeclaredMethods()) {
                // Return type
                classes.add(getClassName(method.getReturnType()));

                // Parameter types
                for (Class<?> paramType : method.getParameterTypes()) {
                    classes.add(getClassName(paramType));
                }

                // Exception types
                for (Class<?> exceptionType : method.getExceptionTypes()) {
                    classes.add(getClassName(exceptionType));
                }
            }

            // Field types
            try {
                for (Field field : clazz.getDeclaredFields()) {
                    classes.add(getClassName(field.getType()));
                }
            } catch (Exception e) {
                System.out.println("skip " + e);
                // Some classes may not allow field access
            }

            // Inner classes
            try {
                for (Class<?> innerClass : clazz.getDeclaredClasses()) {
                    if (Modifier.isPublic(innerClass.getModifiers())
                            || Modifier.isProtected(innerClass.getModifiers())) {
                        classes.add(innerClass.getName());
                    }
                }
            } catch (Exception e) {
                System.out.println("skip " + e);
                // Some classes may not allow inner class access
            }

        } catch (Exception e) {
            System.out.println("skip " + e);
            // Skip classes that can't be introspected
        }

        // Filter out null, empty, primitive types, and reflection classes
        classes.removeIf(className -> className == null || className.isEmpty()
                || className.startsWith("java.lang.reflect") || !className.contains("."));
        return classes;
    }

    private static String getClassName(Class<?> clazz) {
        if (clazz == null || clazz.isPrimitive()) {
            return "";
        }

        if (clazz.isArray()) {
            // Handle array types - get the component type
            Class<?> componentType = clazz.getComponentType();
            while (componentType.isArray()) {
                componentType = componentType.getComponentType();
            }
            return getClassName(componentType);
        }

        return clazz.getName();
    }

    private static String getMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append("(");
        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(paramTypes[i].getSimpleName());
        }
        sb.append(") : ").append(method.getReturnType().getSimpleName());
        return sb.toString();
    }

    private static List<Class<?>> findClasses(File directory, String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    try {
                        Class<?> clazz = Class.forName(className);
                        classes.add(clazz);
                        // Also add inner classes
                        for (Class<?> innerClass : clazz.getDeclaredClasses()) {
                            if (Modifier.isPublic(innerClass.getModifiers())
                                    || Modifier.isProtected(innerClass.getModifiers())) {
                                classes.add(innerClass);
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        // Ignore classes that can't be loaded
                    }
                }
            }
        }
        return classes;
    }

    public static class TypeOrTrait {
        public final boolean isInterface;
        public final Class<?> clazz;
        public final String name;
        public final List<String> methods;
        public final List<String> superInterfaces;
        public final List<String> implementedInterfaceMethods;

        public TypeOrTrait(Class<?> clazz, String name, boolean isInterface, List<String> methods,
                List<String> superInterfaces, List<String> implementedInterfaceMethods) {
            this.clazz = clazz;
            this.name = name;
            this.isInterface = isInterface;
            this.methods = methods;
            this.superInterfaces = superInterfaces;
            this.implementedInterfaceMethods = implementedInterfaceMethods;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(" interfaces " + getImplementedInterfaces());
            sb.append(" interfaceMethods " + getImplementedInterfaceMethods()).append(' ');
            sb.append(name).append(' ').append(isInterface ? "Interface " : "Class ").append(" ");
            if (!superInterfaces.isEmpty()) {
                sb.append("  Super interfaces: ").append(superInterfaces).append(" ");
            }
            if (isInterface && !methods.isEmpty()) {
                sb.append("  Methods: ").append(methods).append(" ");
            }
            if (!isInterface && !implementedInterfaceMethods.isEmpty()) {
                sb.append("  Interface methods: ").append(implementedInterfaceMethods).append(" ");
            }
            return sb.toString();
        }

        int getImplementedInterfaces() {
            if (isInterface) {
                return -1;
            }
            int count = 0;
            for (Class<?> c : clazz.getInterfaces()) {
                if (c.getMethods().length == 0) {
                    continue;
                }
                count++;
            }
            return count;
        }

        int getImplementedInterfaceMethods() {
            if (isInterface) {
                return -1;
            }
            return implementedInterfaceMethods.size();
        }

    }

}
