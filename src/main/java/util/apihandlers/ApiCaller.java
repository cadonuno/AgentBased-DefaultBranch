package util.apihandlers;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import util.*;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ApiCaller {
    private static final String URL_BASE = "api.veracode.com/srcclr";
    private static final String WORKSPACE_LIST_API_URL = "/v3/workspaces";
    private static final String PROJECT_LIST_API_URL = "/projects";
    private static final String SIZE_FILTER = "?size=500";
    private static final String GET_REQUEST = "GET";
    public static final int REQUEST_TIMEOUT = 0;

    public static List<Workspace> getAllWorkspaces(ApiCredentials apiCredentials) {
        return runApi(WORKSPACE_LIST_API_URL + SIZE_FILTER, GET_REQUEST, null, apiCredentials)
                .flatMap(JsonHandler::getWorkspacesFromUrl)
                .orElse(Collections.emptyList());
    }

    public static List<Project> getAllProjects(ApiCredentials apiCredentials, Workspace workspace) {
        return runApi(WORKSPACE_LIST_API_URL + "/" + workspace.getGuid() + PROJECT_LIST_API_URL + SIZE_FILTER,
                GET_REQUEST, null, apiCredentials)
                .flatMap(apiCallResult -> JsonHandler.getProjectsFromUrl(apiCallResult, workspace))
                .orElse(Collections.emptyList());
    }

    private static Optional<JSONObject> runApi(String apiUrl, String requestType,
                                               String jsonParameters, ApiCredentials apiCredentials) {
        try {
            final URL applicationsApiUrl = new URL("https://" + URL_BASE + apiUrl);
            final String authorizationHeader =
                    HmacRequestSigner.getVeracodeAuthorizationHeader(apiCredentials, applicationsApiUrl, requestType);

            final HttpsURLConnection connection = (HttpsURLConnection) applicationsApiUrl.openConnection();
            connection.setConnectTimeout(REQUEST_TIMEOUT);
            connection.setReadTimeout(REQUEST_TIMEOUT);
            connection.setRequestMethod(requestType);
            connection.setRequestProperty("Authorization", authorizationHeader);

            if (jsonParameters != null) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    byte[] input = jsonParameters.getBytes(StandardCharsets.UTF_8);
                    outputStream.write(input, 0, input.length);
                }
            }

            try (InputStream responseInputStream = connection.getInputStream()) {
                return Optional.of(readResponse(responseInputStream));
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException
                | IOException | JSONException e) {
            Logger.log("Unable to run API at: " + apiUrl + "\nWith parameters: " + jsonParameters);
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /*
     * A simple method to read an input stream (containing JSON) to System.out.
     */
    private static JSONObject readResponse(InputStream responseInputStream) throws IOException, JSONException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] responseBytes = new byte[16384];
        int x;
        while ((x = responseInputStream.read(responseBytes, 0, responseBytes.length)) != -1) {
            outputStream.write(responseBytes, 0, x);
        }
        outputStream.flush();
        return new JSONObject(outputStream.toString());
    }
}
