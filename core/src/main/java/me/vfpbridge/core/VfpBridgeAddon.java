package me.vfpbridge.core;

import me.vfpbridge.core.listener.BridgeListener;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;

@AddonMain
public class VfpBridgeAddon extends LabyAddon<VfpBridgeConfiguration> {

  private static VfpBridgeAddon instance;

  public static VfpBridgeAddon instance() {
    return instance;
  }

  @Override
  protected void enable() {
    instance = this;

    this.registerSettingCategory();
    this.registerListener(new BridgeListener(this));

    this.logger().info("ViaFabricPlus Bridge enabled");
  }

  @Override
  protected Class<VfpBridgeConfiguration> configurationClass() {
    return VfpBridgeConfiguration.class;
  }
}
