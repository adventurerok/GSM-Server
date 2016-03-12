package com.ithinkrok.msm.server.web;

import com.ithinkrok.msm.server.Server;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by paul on 12/03/16.
 */
public class WebPanel extends NanoHTTPD {

    private final Server server;

    private final Path webPath;

    public WebPanel(Server server, Path webPath) {
        super(8091);
        this.server = server;
        this.webPath = webPath;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if(uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        if(uri.isEmpty()) {
            uri = "index.html";
        }

        Path file = webPath.resolve(uri);

        if(!Files.exists(file)) return super.serve(session);

        try {
            return newChunkedResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, Files.newInputStream(file));
        } catch (IOException ignored) {
            return super.serve(session);
        }
    }
}
