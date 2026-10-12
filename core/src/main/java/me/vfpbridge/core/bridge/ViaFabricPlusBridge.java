package me.vfpbridge.core.bridge;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Talks to ViaFabricPlus (a Fabric mod) from inside the LabyMod addon, purely through reflection.
 *
 * <p>Supported:
 * <ul>
 *   <li><b>ViaFabricPlus 5.x</b> - public API via {@code ViaFabricPlus.api()} (all menus)</li>
 *   <li><b>ViaFabricPlus 3.x/4.x</b> - legacy API via {@code ViaFabricPlus.getImpl()} (version
 *       picker + settings screen only)</li>
 * </ul>
 */
public final class ViaFabricPlusBridge {

  /** The ViaFabricPlus screens that can be opened. */
  public enum Menu {
    MAIN("ViaFabricPlus menu"),
    SETTINGS("ViaFabricPlus settings"),
    CLASSICUBE("ClassiCube server list"),
    BETACRAFT("BetaCraft server list"),
    REPORT("Report issues");

    private final String title;

    Menu(String title) {
      this.title = title;
    }

    public String title() {
      return this.title;
    }
  }

  /** Outcome of an action, with a message that is safe to show to the player. */
  public record Result(boolean ok, String message) {

    static Result ok(String message) {
      return new Result(true, message);
    }

    static Result fail(String message) {
      return new Result(false, message);
    }
  }

  private static final String P = "com.viaversion.viafabricplus.";

  private static final String NOT_INSTALLED =
      "ViaFabricPlus was not found. Install it (and make sure it matches your Minecraft version).";
  private static final String CONNECTED =
      "Disconnect from the server first - ViaFabricPlus only allows this while you are not connected.";

  private boolean loaded;
  private Class<?> mainClass;
  private Class<?> apiClass;
  private Class<?> baseClass;
  private Class<?> screensClass;
  private Class<?> versionClass;
  private Class<?> screenClass;

  // ------------------------------------------------------------------ availability

  /** {@code true} when ViaFabricPlus is installed, loaded and usable. */
  public boolean isAvailable() {
    return this.init() && this.handle() != null;
  }

  /** {@code true} while the player is connected to a server (version changes are unsafe then). */
  public boolean isConnected() {
    try {
      Handle h = this.handle();
      if (h == null) {
        return false;
      }
      Object connection = h.modern
          ? Reflect.call(h.obj, this.apiClass, "userConnection")
          : Reflect.call(h.obj, this.baseClass, "getPlayNetworkUserConnection");
      return connection != null;
    } catch (Throwable ignored) {
      return false;
    }
  }

  // ------------------------------------------------------------------ versions

  /** Name of the currently selected target version, or {@code null} if unavailable. */
  public String currentVersionName() {
    Object version = this.currentVersion();
    return version == null ? null : this.nameOf(version);
  }

  /**
   * Selects a version by name, e.g. {@code 1.8.9}, {@code 1.12.x}, {@code b1.7.3}, {@code c0.30},
   * {@code auto} or {@code native}.
   */
  public Result setVersion(String query) {
    if (!this.isAvailable()) {
      return Result.fail(NOT_INSTALLED);
    }
    if (query == null || query.isBlank()) {
      return Result.fail("Type a version first, for example 1.8.9");
    }
    Object version = this.resolve(query);
    if (version == null) {
      return Result.fail("Unknown version '" + query.trim()
          + "'. Try for example 1.8.9, 1.12.2, 1.21.x or b1.7.3 - or use the Newer/Older buttons.");
    }
    return this.apply(version);
  }

  /** Moves to the next newer ({@code +1}) or older ({@code -1}) version. */
  public Result step(int direction) {
    if (!this.isAvailable()) {
      return Result.fail(NOT_INSTALLED);
    }
    List<Object> versions = this.versions();
    if (versions.isEmpty()) {
      return Result.fail("ViaFabricPlus returned no versions.");
    }
    Object current = this.currentVersion();
    int index = current == null ? -1 : versions.indexOf(current);
    if (index < 0) {
      Object nativeVersion = this.nativeVersion();
      index = nativeVersion == null ? 0 : Math.max(0, versions.indexOf(nativeVersion));
    }
    // list is ordered newest -> oldest
    int target = index - direction;
    if (target < 0) {
      return Result.fail("Already on the newest version.");
    }
    if (target >= versions.size()) {
      return Result.fail("Already on the oldest version.");
    }
    return this.apply(versions.get(target));
  }

  /** Switches back to the version of the installed Minecraft client (no translation). */
  public Result resetToNative() {
    if (!this.isAvailable()) {
      return Result.fail(NOT_INSTALLED);
    }
    Object nativeVersion = this.nativeVersion();
    if (nativeVersion == null) {
      return Result.fail("Could not determine the native version of this ViaFabricPlus build.");
    }
    return this.apply(nativeVersion);
  }

  private Result apply(Object version) {
    if (this.isConnected()) {
      return Result.fail(CONNECTED);
    }
    try {
      Handle h = this.handle();
      if (h == null) {
        return Result.fail(NOT_INSTALLED);
      }
      if (h.modern) {
        Reflect.call(h.obj, this.apiClass, "setTargetVersion", version);
      } else {
        Reflect.call(h.obj, this.baseClass, "setTargetVersion", version);
      }
      return Result.ok("Target version: " + this.nameOf(version));
    } catch (Throwable t) {
      return Result.fail("ViaFabricPlus rejected the change: " + root(t));
    }
  }

  /** All known versions, newest first. */
  public List<Object> versions() {
    if (!this.init()) {
      return Collections.emptyList();
    }
    try {
      List<Object> result = new ArrayList<>();
      Object raw;
      boolean reversed = Reflect.has(this.versionClass, "getReversedProtocols", 0);
      raw = Reflect.call(null, this.versionClass,
          reversed ? "getReversedProtocols" : "getProtocols");
      for (Object version : (Collection<?>) raw) {
        if (this.isKnown(version)) {
          result.add(version);
        }
      }
      if (!reversed) {
        Collections.reverse(result);
      }
      return result;
    } catch (Throwable t) {
      return Collections.emptyList();
    }
  }

  private boolean isKnown(Object version) {
    try {
      if (Reflect.has(this.versionClass, "isKnown", 0)) {
        return (boolean) Reflect.call(version, this.versionClass, "isKnown");
      }
    } catch (Throwable ignored) {
      // treat as known
    }
    return true;
  }

  private Object resolve(String rawQuery) {
    String query = rawQuery.trim().toLowerCase(Locale.ROOT);

    if (query.equals("native") || query.equals("default") || query.equals("latest")
        || query.equals("reset")) {
      return this.nativeVersion();
    }
    Object auto = this.autoDetectVersion();
    if (auto != null && (query.equals("auto") || query.startsWith("auto detect"))) {
      return auto;
    }

    List<Object> versions = this.versions();

    // 1) exact name ("1.8.x"), 2) exact included version ("1.8.9")
    for (Object version : versions) {
      if (this.nameOf(version).equalsIgnoreCase(query)) {
        return version;
      }
    }
    for (Object version : versions) {
      for (String included : this.includedVersions(version)) {
        if (included.equalsIgnoreCase(query)) {
          return version;
        }
      }
    }

    // 3) unique "starts with" / "contains" match ("1.12" -> 1.12.x)
    Object match = uniqueMatch(versions, query, true);
    return match != null ? match : uniqueMatch(versions, query, false);
  }

  private Object uniqueMatch(List<Object> versions, String query, boolean startsWith) {
    Object found = null;
    for (Object version : versions) {
      boolean hit = test(this.nameOf(version), query, startsWith);
      if (!hit) {
        for (String included : this.includedVersions(version)) {
          if (test(included, query, startsWith)) {
            hit = true;
            break;
          }
        }
      }
      if (hit) {
        if (found != null) {
          return null; // ambiguous
        }
        found = version;
      }
    }
    return found;
  }

  private static boolean test(String value, String query, boolean startsWith) {
    String lower = value.toLowerCase(Locale.ROOT);
    return startsWith ? lower.startsWith(query) : lower.contains(query);
  }

  private List<String> includedVersions(Object version) {
    try {
      if (Reflect.has(this.versionClass, "getIncludedVersions", 0)) {
        Object raw = Reflect.call(version, this.versionClass, "getIncludedVersions");
        List<String> result = new ArrayList<>();
        for (Object o : (Collection<?>) raw) {
          result.add(String.valueOf(o));
        }
        return result;
      }
    } catch (Throwable ignored) {
      // none
    }
    return Collections.emptyList();
  }

  private String nameOf(Object version) {
    try {
      return String.valueOf(Reflect.call(version, this.versionClass, "getName"));
    } catch (Throwable t) {
      return String.valueOf(version);
    }
  }

  private Object currentVersion() {
    try {
      Handle h = this.handle();
      if (h == null) {
        return null;
      }
      return h.modern
          ? Reflect.call(h.obj, this.apiClass, "targetVersion")
          : Reflect.call(h.obj, this.baseClass, "getTargetVersion");
    } catch (Throwable t) {
      return null;
    }
  }

  private Object nativeVersion() {
    for (String name : new String[] {
        P + "protocoltranslator.ProtocolTranslationImpl", // 5.x
        P + "protocoltranslator.ProtocolTranslator" // 3.x / 4.x
    }) {
      Class<?> cls = Reflect.load(name);
      if (cls != null) {
        Object value = Reflect.staticField(cls, "NATIVE_VERSION");
        if (value != null) {
          return value;
        }
      }
    }
    return null;
  }

  private Object autoDetectVersion() {
    Class<?> cls = Reflect.load(P + "api.protocoltranslator.ProtocolTranslation");
    return cls == null ? null : Reflect.staticField(cls, "AUTO_DETECT_VERSION");
  }

  // ------------------------------------------------------------------ menus

  /** Opens a ViaFabricPlus screen on top of whatever screen is currently shown. */
  public Result open(Menu menu) {
    if (!this.isAvailable()) {
      return Result.fail(NOT_INSTALLED);
    }
    try {
      Handle h = this.handle();
      Object parent = this.currentScreen();

      switch (menu) {
        case MAIN -> {
          if (h.modern) {
            Reflect.call(this.screens(h), this.screensClass, "openViaFabricPlusScreen", parent);
          } else {
            Reflect.call(h.obj, this.baseClass, "openProtocolSelectionScreen", parent);
          }
        }
        case SETTINGS -> {
          if (h.modern) {
            Reflect.call(this.screens(h), this.screensClass, "openSettingsScreen", parent);
          } else {
            Reflect.call(h.obj, this.baseClass, "openSettingsScreen", parent);
          }
        }
        case CLASSICUBE, BETACRAFT -> {
          if (!h.modern) {
            return Result.fail(menu.title() + " needs ViaFabricPlus 5 or newer.");
          }
          if (this.isConnected()) {
            return Result.fail(CONNECTED);
          }
          Reflect.call(this.screens(h), this.screensClass,
              menu == Menu.CLASSICUBE
                  ? "openClassiCubeServerListScreen"
                  : "openBetaCraftServerListScreen",
              parent);
        }
        case REPORT -> {
          // Not part of the public API, so this one reaches into ViaFabricPlus internals.
          Class<?> impl = Reflect.load(P + "ViaFabricPlusImpl");
          if (impl == null) {
            return Result.fail(menu.title() + " is not available in this ViaFabricPlus version.");
          }
          Object instance = Reflect.call(null, impl, "impl");
          Object screens = Reflect.call(instance, instance.getClass(), "screens");
          Object screen = Reflect.call(screens, screens.getClass(), "reportIssuesScreen");
          Reflect.call(screen, screen.getClass(), "open", parent);
        }
        default -> {
          return Result.fail("Unknown menu.");
        }
      }
      return Result.ok("Opened " + menu.title());
    } catch (Throwable t) {
      return Result.fail("Could not open " + menu.title() + ": " + root(t));
    }
  }

  private Object screens(Handle h) throws Exception {
    return Reflect.call(h.obj, this.apiClass, "screens");
  }

  /**
   * The screen currently shown, so ViaFabricPlus can return to it when you press Back.
   *
   * <p>Best effort: if it cannot be found, {@code null} is passed and Back simply closes the
   * ViaFabricPlus screen (Minecraft then shows the title screen / the game).
   */
  private Object currentScreen() {
    try {
      Class<?> screen = this.screenClass;
      String[][] candidates = {
          {"net.minecraft.client.Minecraft", "getInstance", "gui", "screen", "getScreen"},
          {"net.minecraft.class_310", "method_1551", "field_1705", "field_1755", "method_1507"}
      };
      for (String[] c : candidates) {
        Class<?> mc = Reflect.load(c[0]);
        if (mc == null) {
          continue;
        }
        Object instance;
        try {
          instance = Reflect.call(null, mc, c[1]);
        } catch (Throwable t) {
          continue;
        }
        // Minecraft#screen field / screen() / getScreen()
        Object found = firstScreen(instance, mc, screen, c[3], c[4]);
        if (found != null) {
          return found;
        }
        // Newer versions keep the screen on Minecraft#gui
        Object gui = readMember(instance, mc, c[2]);
        if (gui != null) {
          found = firstScreen(gui, gui.getClass(), screen, c[3], c[4]);
          if (found != null) {
            return found;
          }
        }
      }
    } catch (Throwable ignored) {
      // fall through
    }
    return null;
  }

  private static Object firstScreen(Object owner, Class<?> type, Class<?> screenType,
      String name, String getter) {
    for (String member : new String[] {name, getter}) {
      Object value = readMember(owner, type, member);
      if (value != null && (screenType == null || screenType.isInstance(value))) {
        return value;
      }
    }
    return null;
  }

  private static Object readMember(Object owner, Class<?> type, String name) {
    try {
      return type.getField(name).get(owner);
    } catch (Throwable ignored) {
      // not a public field
    }
    try {
      if (Reflect.has(type, name, 0)) {
        return Reflect.call(owner, type, name);
      }
    } catch (Throwable ignored) {
      // not a method either
    }
    return null;
  }

  // ------------------------------------------------------------------ change listener

  /**
   * Calls {@code listener} with the new version name whenever the version changes - including
   * changes made inside ViaFabricPlus' own screens or by joining a server with a forced version.
   *
   * @return whether the listener could be registered
   */
  public boolean addVersionListener(Consumer<String> listener) {
    try {
      Handle h = this.handle();
      if (h == null) {
        return false;
      }
      Class<?> declaring = h.modern ? this.apiClass : this.baseClass;
      String name = h.modern ? "addChangeProtocolVersionListener"
          : "registerOnChangeProtocolVersionCallback";
      Method method = Reflect.find(declaring, name, 1);
      if (method == null) {
        return false;
      }
      BiConsumer<Object, Object> callback = (oldVersion, newVersion) -> {
        try {
          listener.accept(this.nameOf(newVersion));
        } catch (Throwable ignored) {
          // never break ViaFabricPlus' listener loop
        }
      };
      Class<?> parameter = method.getParameterTypes()[0];
      Object argument = parameter.isInstance(callback)
          ? callback
          : Proxy.newProxyInstance(parameter.getClassLoader(), new Class<?>[] {parameter},
              (proxy, m, args) -> {
                if (m.getDeclaringClass() == Object.class) {
                  return switch (m.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> "VfpBridgeCallback";
                  };
                }
                if (args != null && args.length == 2) {
                  callback.accept(args[0], args[1]);
                }
                return null;
              });
      method.invoke(h.obj, argument);
      return true;
    } catch (Throwable t) {
      return false;
    }
  }

  // ------------------------------------------------------------------ internals

  private record Handle(Object obj, boolean modern) {
  }

  private boolean init() {
    if (this.loaded) {
      return true;
    }
    this.mainClass = Reflect.load(P + "ViaFabricPlus");
    this.versionClass = Reflect.load("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
    if (this.mainClass == null || this.versionClass == null) {
      return false;
    }
    this.apiClass = Reflect.load(P + "api.ViaFabricPlusAPI");
    this.baseClass = Reflect.load(P + "api.ViaFabricPlusBase");
    this.screensClass = Reflect.load(P + "api.screen.Screens");
    this.screenClass = firstNonNull(
        Reflect.load("net.minecraft.client.gui.screens.Screen"), // Mojang names (26.x+)
        Reflect.load("net.minecraft.class_437")); // Fabric intermediary
    this.loaded = true;
    return true;
  }

  /** Resolves the live API object. Re-checked on every call because init order is not fixed. */
  private Handle handle() {
    if (!this.init()) {
      return null;
    }
    if (this.apiClass != null && Reflect.has(this.mainClass, "api", 0)) {
      try {
        Object api = Reflect.call(null, this.mainClass, "api");
        if (api != null) {
          return new Handle(api, true);
        }
      } catch (Throwable ignored) {
        // not initialised yet, or not the 5.x API
      }
    }
    if (this.baseClass != null && Reflect.has(this.mainClass, "getImpl", 0)) {
      try {
        Object legacy = Reflect.call(null, this.mainClass, "getImpl");
        if (legacy != null) {
          return new Handle(legacy, false);
        }
      } catch (Throwable ignored) {
        // ignore
      }
    }
    return null;
  }

  @SafeVarargs
  private static <T> T firstNonNull(T... values) {
    for (T value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private static String root(Throwable t) {
    Throwable cause = t;
    while (cause.getCause() != null && cause.getCause() != cause) {
      cause = cause.getCause();
    }
    String message = cause.getMessage();
    return message == null ? cause.getClass().getSimpleName() : message;
  }
}
