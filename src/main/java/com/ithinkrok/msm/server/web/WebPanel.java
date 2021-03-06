package com.ithinkrok.msm.server.web;

import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.web.api.ClientNamesMethod;
import com.ithinkrok.msm.server.web.api.ClientResourceUsageMethod;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.JsonConfigIO;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by paul on 12/03/16.
 */
public class WebPanel extends NanoHTTPD {

    private final Server server;

    private final Path webPath;

    private final Map<String, ApiMethod> apiMethodMap = new ConcurrentHashMap<>();

    public WebPanel(Server server, Path webPath) {
        super(8091);
        this.server = server;
        this.webPath = webPath;

        loadApiMethods();
    }

    private void loadApiMethods() {
        apiMethodMap.put("client_names", new ClientNamesMethod());
        apiMethodMap.put("client/resource_usage", new ClientResourceUsageMethod());
    }

    @Override
    public Response serve(IHTTPSession session) {
        Path uriPath = getUriPath(session);

        if(uriPath.getNameCount() > 1 && uriPath.getName(0).toString().equals("api")) {
            return serveApi(session, uriPath.getName(0).relativize(uriPath));
        }

        return serveFile(session, uriPath);
    }

    private Response serveApi(IHTTPSession session, Path path) {
        String pathName = path.toString();

        ApiMethod method = apiMethodMap.get(pathName);

        String message;

        if(method != null) {
            Config config = method.call(session, server);

            message = JsonConfigIO.dumpConfig(config);

            if(message == null) message = "{}";
        } else {
            message = "{}";
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", message);
    }

    public Response serveFile(IHTTPSession session, Path uriPath) {
        Path file = webPath.resolve(uriPath);

        if(!Files.exists(file)) return super.serve(session);

        try {
            String type = Files.probeContentType(file);
            InputStream input = Files.newInputStream(file);

            return newChunkedResponse(Response.Status.OK, type, input);
        } catch (IOException ignored) {
            return super.serve(session);
        }
    }

    public Path getUriPath(IHTTPSession session) {
        String uri = session.getUri();
        if(uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        if(uri.isEmpty()) {
            uri = "index.html";
        }

        return Paths.get(uri);
    }
}
