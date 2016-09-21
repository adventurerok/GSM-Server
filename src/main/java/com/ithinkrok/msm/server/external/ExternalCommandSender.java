package com.ithinkrok.msm.server.external;

import com.ithinkrok.util.command.CustomCommandSender;
import com.ithinkrok.util.lang.LanguageLookup;

/**
 * Created by paul on 21/09/16.
 */
public class ExternalCommandSender implements CustomCommandSender {

    private final External external;

    public ExternalCommandSender(External external) {
        this.external = external;
    }

    public External getExternal() {
        return external;
    }

    @Override
    public void sendMessageNoPrefix(String message) {
        if(external instanceof ExternalChat) {
            ((ExternalChat) external).sendMessageNoPrefix(message);
        }
    }

    @Override
    public LanguageLookup getLanguageLookup() {
        return external.getConnectedTo().getLanguageLookup();
    }
}
