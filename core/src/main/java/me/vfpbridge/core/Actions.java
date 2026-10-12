package me.vfpbridge.core;

import me.vfpbridge.core.bridge.ViaFabricPlusBridge;
import me.vfpbridge.core.bridge.ViaFabricPlusBridge.Menu;
import me.vfpbridge.core.bridge.ViaFabricPlusBridge.Result;
import net.labymod.api.client.component.Component;
import net.labymod.api.notification.Notification;

/** Everything the settings buttons, the hotkey and the startup hook can do. */
public final class Actions {

  private static final ViaFabricPlusBridge BRIDGE = new ViaFabricPlusBridge();

  private Actions() {
  }

  public static ViaFabricPlusBridge bridge() {
    return BRIDGE;
  }

  public static void open(Menu menu) {
    Result result = BRIDGE.open(menu);
    if (!result.ok()) {
      notify(result.message());
    }
  }

  /** Applies the version typed into the settings text field. */
  public static void applyTyped() {
    VfpBridgeConfiguration config = config();
    if (config == null) {
      return;
    }
    finishVersionChange(BRIDGE.setVersion(config.targetVersion().get()));
  }

  public static void newerVersion() {
    finishVersionChange(BRIDGE.step(+1));
  }

  public static void olderVersion() {
    finishVersionChange(BRIDGE.step(-1));
  }

  public static void resetToNative() {
    finishVersionChange(BRIDGE.resetToNative());
  }

  private static void finishVersionChange(Result result) {
    if (result.ok()) {
      syncField(BRIDGE.currentVersionName());
    }
    // Failures always show; successes only when the user wants notifications.
    VfpBridgeConfiguration config = config();
    if (!result.ok() || config == null || config.notifications().get()) {
      notify(result.message());
    }
  }

  /** Mirrors the active ViaFabricPlus version into the settings text field. */
  public static void syncField(String versionName) {
    VfpBridgeConfiguration config = config();
    if (config != null && versionName != null && !versionName.equals(config.targetVersion().get())) {
      config.targetVersion().set(versionName);
    }
  }

  public static void notify(String message) {
    Notification.builder()
        .title(Component.text("ViaFabricPlus Bridge"))
        .text(Component.text(message))
        .duration(4500)
        .buildAndPush();
  }

  private static VfpBridgeConfiguration config() {
    VfpBridgeAddon addon = VfpBridgeAddon.instance();
    return addon == null ? null : addon.configuration();
  }
}
