package com.ithinkrok.msm.server.web;

import com.ithinkrok.msm.server.Server;
import fi.iki.elonen.NanoHTTPD;

/**
 * Created by paul on 12/03/16.
 */
public class WebPanel extends NanoHTTPD {

    private final Server server;

    public WebPanel(Server server) {
        super(8091);
        this.server = server;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Hello Everyone</h1>";

        msg += "<p>Hello there</p>";

        msg += "<p>" + server.getClients().size() + " clients connected to GSM</p>";

        msg += "</body></html>";

        return newFixedLengthResponse(msg);
    }
}
