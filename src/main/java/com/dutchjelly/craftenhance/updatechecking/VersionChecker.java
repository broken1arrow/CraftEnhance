package com.dutchjelly.craftenhance.updatechecking;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.messaging.Messenger;

public class VersionChecker {

	private CraftEnhance plugin;

	public static VersionChecker init(CraftEnhance plugin) {
		VersionChecker checker = new VersionChecker();
		checker.plugin = plugin;
		return checker;
	}

	public void runUpdateCheck() {
		if (!plugin.getConfig().getBoolean("enable-updatechecker")) return;

		GithubLoader loader = GithubLoader.init(this);
		loader.readVersion();
		String version = loader.getVersion();
		if (version == null) return;
		version = version.trim();
		String currentVersion = versionCheck(version);
		if (!isOutDated(currentVersion)) {
			Messenger.Message("CraftEnhance is up to date.");
		} else {
			Messenger.Message("There's a new version (" + currentVersion + ") of the plugin available on https://dev.bukkit.org/projects/craftenhance/files.");
		}
	}

	public String versionCheck(String version) {
		String[] currentVersions = version.split("\n");
		/*for (int i = 0; i < currentVersions.length; i++) {
			String ver = currentVersions[i];
			String[] values = ver.split("\\.");
			for (int e = 0; e < values.length; e++) {
				int versionNumber = Integer.parseInt(values[i]);
				if (versionNumber >= value)
					value = versionNumber;
			}
		}*/
		return currentVersions[currentVersions.length - 1];
	}

	public boolean runVersionCheck() {
		String serverVersion = plugin.getServer().getBukkitVersion();
		Messenger.Message("Running a version check to check that the server is compatible with game version " + String.join(", ", Adapter.CompatibleVersions()) + ".");
		for (String version : Adapter.CompatibleVersions()) {
			if (serverVersion.contains(version)) {
				Messenger.Message("The correct version is installed.");
				return true;
			}
		}
		Messenger.Message("");
		Messenger.Message("!! Incompatibility found !!");
		Messenger.Message("The installed version of CraftEnhance only supports spigot/bukkit versions \"" + String.join(", ", Adapter.CompatibleVersions()) + "\"");
		Messenger.Message("while your server is running " + serverVersion + ".");
		Messenger.Message("The correct version can be installed here: https://dev.bukkit.org/projects/craftenhance/files");
		Messenger.Message("When installing the plugin make sure that the game version matches your bukkit or spigot version.");
		Messenger.Message("Please note that this incompatibility could cause duping glitches.");
		Messenger.Message("So because the incorrect plugin version is being used, the plugin has to be disabled.");
		return false;
	}


	public CraftEnhance getPlugin() {
		return plugin;
	}

	private boolean isOutDated(String version) {
		String currentVersion = plugin.getDescription().getVersion();
		return !version.equalsIgnoreCase(currentVersion);
		//return !Arrays.stream(version.split("\n")).anyMatch(x -> x.equalsIgnoreCase(currentVersion));
	}
}
