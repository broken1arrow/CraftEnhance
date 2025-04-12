package com.dutchjelly.craftenhance.updatechecking;

import com.dutchjelly.bukkitadapter.Adapter;
import com.dutchjelly.craftenhance.CraftEnhance;
import com.dutchjelly.craftenhance.messaging.Messenger;
import lombok.Getter;

public class VersionChecker {

    private CraftEnhance plugin;
    private String serverVersion;
    private int currentServerVersion;

    public static VersionChecker init(final CraftEnhance plugin) {
        final VersionChecker checker = new VersionChecker();
        checker.serverVersion = plugin.getServer().getBukkitVersion();
        String version = checker.serverVersion.split("\\.")[1];
        if (version.contains("-"))
            version =version.substring(0,version.indexOf('-'));
        checker.currentServerVersion = Integer.parseInt(version);
        checker.plugin = plugin;
        return checker;
    }

    public void runUpdateCheck() {
        if (!plugin.getConfig().getBoolean("enable-updatechecker")) return;

        final GithubLoader loader = GithubLoader.init(this);
        loader.readVersion();
        String version = loader.getVersion();
        if (version == null) return;
        version = version.trim();
        final String currentVersion = versionCheck(version);
        if (!isOutDated(currentVersion)) {
            Messenger.Message("CraftEnhance is up to date.");
        } else {
            Messenger.Message("There's a new version (" + currentVersion + ") of the plugin available on https://www.spigotmc.org/resources/1-9-1-19-custom-recipes-and-crafting-craftenhance.65058/ or https://dev.bukkit.org/projects/craftenhance/files .");
        }
    }

    public String versionCheck(final String version) {
        final String[] currentVersions = version.split("\n");
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
        Messenger.Message("Running a version check to check that the server is compatible with game version " + String.join(", ", Adapter.CompatibleVersions()) + ".");
        for (final String version : Adapter.CompatibleVersions()) {
            if (serverVersion.contains(version)) {
                Messenger.Message("The correct version is installed.");
                return true;
            }
        }
        Messenger.Message("");
        Messenger.Message("!! Incompatibility found !!");
        Messenger.Message("The installed version of CraftEnhance only supports spigot/bukkit versions \"" + String.join(", ", Adapter.CompatibleVersions()) + "\"");
        Messenger.Message("while your server is running " + serverVersion + ".");
        Messenger.Message("The correct version can be installed here: https://www.spigotmc.org/resources/1-9-1-19-custom-recipes-and-crafting-craftenhance.65058/ or https://dev.bukkit.org/projects/craftenhance/files .");
        Messenger.Message("Alternatively do you find latest here: https://github.com/broken1arrow/CraftEnhance/releases .");
        Messenger.Message("When installing the plugin make sure that the game version matches your bukkit or spigot version.");
        Messenger.Message("Please note that this incompatibility could cause duping glitches.");
        //Messenger.Message("So because the incorrect plugin version is being used, the plugin has to be disabled.");
        return false;
    }

    public CraftEnhance getPlugin() {
        return plugin;
    }

    private boolean isOutDated(final String version) {
        final String currentVersion = plugin.getDescription().getVersion();
        return !version.equalsIgnoreCase(currentVersion);
        //return !Arrays.stream(version.split("\n")).anyMatch(x -> x.equalsIgnoreCase(currentVersion));
    }

    public boolean equals(final ServerVersion version) {
        return serverVersion(version) == 0;
    }

    public boolean newerThan(final ServerVersion version) {

        return serverVersion(version) > 0;
    }

    public boolean olderThan(final ServerVersion version) {
        return serverVersion(version) < 0;
    }

    public int serverVersion(final ServerVersion version) {
        return this.currentServerVersion - version.getVersion();
    }

    public enum ServerVersion {
        v1_21(21),
        v1_20(20),
        v1_19(19),
        v1_18(18),
        v1_17(17),
        v1_16(16),
        v1_15(15),
        v1_14(14),
        v1_13(13),
        v1_12(12),
        v1_11(11),
        v1_10(10),
        v1_9(9),
        v1_8(8),
        v1_7(7),
        v1_6(6),
        v1_5(5),
        v1_4(4),
        v1_3_AND_BELOW(3);
        @Getter
        private final int version;

        ServerVersion(final int version) {
            this.version = version;

        }

    }
}
