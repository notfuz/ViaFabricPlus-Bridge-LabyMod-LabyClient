package me.vfpbridge.core.listener;

import me.vfpbridge.core.Actions;
import me.vfpbridge.core.VfpBridgeAddon;
import me.vfpbridge.core.VfpBridgeConfiguration;
import me.vfpbridge.core.bridge.ViaFabricPlusBridge;
import me.vfpbridge.core.bridge.ViaFabricPlusBridge.Menu;
import me.vfpbridge.core.bridge.ViaFabricPlusBridge.Result;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;
import net.labymod.api.event.client.lifecycle.GameTickEvent;

public class BridgeListener {

  /** ~30 seconds of ticks to wait for ViaFabricPlus to finish loading before giving up. */
  private static final int MAX_WAIT_TICKS = 600;

  private final VfpBridgeAddon addon;
  private boolean started;
  private int waited;

  public BridgeListener(VfpBridgeAddon addon) {
    this.addon = addon;
  }

  /** One-time hookup once ViaFabricPlus is ready: sync the field and re-apply the saved version. */
  @Subscribe
  public void onGameTick(GameTickEvent event) {
    if (this.started || event.phase() != Phase.POST) {
      return;
    }

    ViaFabricPlusBridge bridge = Actions.bridge();
    if (!bridge.isAvailable()) {
      if (++this.waited >= MAX_WAIT_TICKS) {
        this.started = true;
        this.addon.logger().warn(
            "ViaFabricPlus was not found - the version/menu buttons will do nothing until it is installed.");
      }
      return;
    }
    this.started = true;

    // Keep the settings text field in sync with changes made inside ViaFabricPlus' own screens.
    bridge.addVersionListener(Actions::syncField);

    VfpBridgeConfiguration config = this.addon.configuration();
    String saved = config.targetVersion().get();
    if (config.enabled().get() && config.applyOnStartup().get() && saved != null && !saved.isBlank()) {
      Result result = bridge.setVersion(saved);
      if (!result.ok()) {
        Actions.notify(result.message());
      }
    } else {
      Actions.syncField(bridge.currentVersionName());
    }
  }

  /** Optional hotkey that opens the ViaFabricPlus menu. */
  @Subscribe
  public void onKey(KeyEvent event) {
    VfpBridgeConfiguration config = this.addon.configuration();
    if (!config.enabled().get() || event.state() != KeyEvent.State.PRESS) {
      return;
    }
    Key configured = config.openMenuKey().get();
    if (configured == null || configured == Key.NONE || !configured.equals(event.key())) {
      return;
    }
    Actions.open(Menu.MAIN);
  }
}
