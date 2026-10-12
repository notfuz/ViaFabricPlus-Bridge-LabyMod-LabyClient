package me.vfpbridge.core;

import me.vfpbridge.core.bridge.ViaFabricPlusBridge.Menu;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.KeybindWidget.KeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.configuration.settings.annotation.SettingSection;
import net.labymod.api.util.MethodOrder;

@ConfigName("settings")
public class VfpBridgeConfiguration extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  // ---------------------------------------------------------------- ViaFabricPlus menus
  // (buttons sit directly below the "Enabled" switch; they work even while the addon is "disabled")

  @MethodOrder(after = "enabled")
  @ButtonSetting
  public void openMenu(Setting setting) {
    Actions.open(Menu.MAIN);
  }

  @MethodOrder(after = "openMenu")
  @ButtonSetting
  public void openSettings(Setting setting) {
    Actions.open(Menu.SETTINGS);
  }

  @MethodOrder(after = "openSettings")
  @ButtonSetting
  public void openClassiCube(Setting setting) {
    Actions.open(Menu.CLASSICUBE);
  }

  @MethodOrder(after = "openClassiCube")
  @ButtonSetting
  public void openBetaCraft(Setting setting) {
    Actions.open(Menu.BETACRAFT);
  }

  @MethodOrder(after = "openBetaCraft")
  @ButtonSetting
  public void openReport(Setting setting) {
    Actions.open(Menu.REPORT);
  }

  // ---------------------------------------------------------------- version

  @SettingSection("version")
  @TextFieldSetting
  private final ConfigProperty<String> targetVersion = new ConfigProperty<>("");

  @MethodOrder(after = "targetVersion")
  @ButtonSetting
  public void applyVersion(Setting setting) {
    Actions.applyTyped();
  }

  @MethodOrder(after = "applyVersion")
  @ButtonSetting
  public void newerVersion(Setting setting) {
    Actions.newerVersion();
  }

  @MethodOrder(after = "newerVersion")
  @ButtonSetting
  public void olderVersion(Setting setting) {
    Actions.olderVersion();
  }

  @MethodOrder(after = "olderVersion")
  @ButtonSetting
  public void resetVersion(Setting setting) {
    Actions.resetToNative();
  }

  // ---------------------------------------------------------------- options

  @SettingSection("options")
  @KeyBindSetting
  private final ConfigProperty<Key> openMenuKey = new ConfigProperty<>(Key.NONE);

  @SwitchSetting
  private final ConfigProperty<Boolean> applyOnStartup = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> notifications = new ConfigProperty<>(true);

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<String> targetVersion() {
    return this.targetVersion;
  }

  public ConfigProperty<Key> openMenuKey() {
    return this.openMenuKey;
  }

  public ConfigProperty<Boolean> applyOnStartup() {
    return this.applyOnStartup;
  }

  public ConfigProperty<Boolean> notifications() {
    return this.notifications;
  }
}
