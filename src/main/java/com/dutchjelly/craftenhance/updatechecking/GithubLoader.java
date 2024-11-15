package com.dutchjelly.craftenhance.updatechecking;


import com.dutchjelly.craftenhance.messaging.Messenger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class GithubLoader {

    private final String rawUrl = "https://raw.githubusercontent.com/broken1arrow/CraftEnhance/master/recent_version.txt";//"https://raw.githubusercontent.com/DutchJelly/CraftEnhance/master/recent_version.txt";

    private URLConnection connection;
    private VersionChecker checker;
    private String version;

    public static GithubLoader init(final VersionChecker checker){
        final GithubLoader loader = new GithubLoader();
        loader.checker = checker;
        if(!loader.openConnection()) return null;

        return loader;
    }

    public String getVersion(){
        return version;
    }

    public void readVersion(){
        final InputStream inputStream;
        final ByteArrayOutputStream outputStream;
        try {
            outputStream = new ByteArrayOutputStream();
            inputStream = connection.getInputStream();
            final byte[] buffer = new byte[128];
            int i;
            while((i = inputStream.read(buffer)) != -1)
                outputStream.write(buffer, 0, i);
            version = new String(outputStream.toByteArray(), "UTF-8");
        } catch (final Exception e) {
            Messenger.Message("(fatal) The update checker could not extract the version from the url connection.");
        }
    }

    private boolean openConnection(){
        try {
            connection = (new URL(rawUrl)).openConnection();
        } catch (final Exception e) {
            Messenger.Message("(fatal) The update checker could not open URL connection.");
            return false;
        }
        return true;
    }

}
