package me.vfpbridge.core.bridge;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Tiny reflection helpers.
 *
 * <p>ViaFabricPlus is a normal Fabric mod, so the addon never links against it at compile time.
 * Everything goes through reflection, which means the addon keeps compiling (and mostly working)
 * no matter which ViaFabricPlus version the user has installed.
 */
final class Reflect {

  private Reflect() {
  }

  /** Loads a class through every class loader that could possibly know the Fabric mod classes. */
  static Class<?> load(String name) {
    ClassLoader[] loaders = {
        Thread.currentThread().getContextClassLoader(),
        Reflect.class.getClassLoader(),
        ClassLoader.getSystemClassLoader()
    };
    for (ClassLoader loader : loaders) {
      if (loader == null) {
        continue;
      }
      try {
        return Class.forName(name, true, loader);
      } catch (Throwable ignored) {
        // try next loader
      }
    }
    return null;
  }

  /**
   * Calls a public method by name and argument count.
   *
   * <p>The method is looked up on {@code declaring} (preferably a public interface or class) rather
   * than on {@code target.getClass()}, because the implementation classes are often package-private
   * and would reject the call.
   */
  static Object call(Object target, Class<?> declaring, String name, Object... args)
      throws Exception {
    Method method = find(declaring, name, args.length);
    if (method == null) {
      throw new NoSuchMethodException(declaring.getName() + "#" + name + "/" + args.length);
    }
    method.setAccessible(true);
    return method.invoke(Modifier.isStatic(method.getModifiers()) ? null : target, args);
  }

  static Method find(Class<?> declaring, String name, int argCount) {
    for (Method method : declaring.getMethods()) {
      if (method.getName().equals(name) && method.getParameterCount() == argCount) {
        return method;
      }
    }
    return null;
  }

  static boolean has(Class<?> declaring, String name, int argCount) {
    return declaring != null && find(declaring, name, argCount) != null;
  }

  static Object staticField(Class<?> declaring, String name) {
    try {
      return declaring.getField(name).get(null);
    } catch (Throwable ignored) {
      return null;
    }
  }
}
