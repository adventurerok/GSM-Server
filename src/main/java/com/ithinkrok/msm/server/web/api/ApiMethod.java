package com.ithinkrok.msm.server.web.api;

import com.ithinkrok.msm.server.Server;
import com.ithinkrok.util.config.Config;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

/**
 * Created by paul on 12/03/16.
 */
public interface ApiMethod {

    Config call(IHTTPSession session, Server server);
}
