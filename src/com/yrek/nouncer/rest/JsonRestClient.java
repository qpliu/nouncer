package com.yrek.nouncer.rest;

import android.util.JsonReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class JsonRestClient {
    public static void request(URL url, Map<String,List<String>> headers, Map<String,List<String>> parameters, ResponseReader responseReader) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(parameters != null);
        if (headers != null) {
            for (Map.Entry<String,List<String>> e : headers.entrySet()) {
                for (String v : e.getValue()) {
                    connection.addRequestProperty(e.getKey(), v);
                }
            }
        }
        if (parameters != null) {
            connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            OutputStream out = null;
            try {
                out = connection.getOutputStream();
                OutputStreamWriter w = new OutputStreamWriter(out, "UTF-8");
                String sep = "";
                for (Map.Entry<String,List<String>> e : parameters.entrySet()) {
                    String k = URLEncoder.encode(e.getKey(), "UTF-8");
                    for (String v : e.getValue()) {
                        w.append(sep).append(k).append('=').append(URLEncoder.encode(v, "UTF-8"));
                        sep = "&";
                    }
                }
                w.flush();
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }

        boolean error = false;
        int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            responseCode = connection.getResponseCode();
            error = true;
        }

        if (error || responseCode >= 400) {
            InputStream err = null;
            try {
                err = connection.getErrorStream();
                responseReader.onError(responseCode, err);
            } finally {
                if (err != null) {
                    err.close();
                }
            }
        } else {
            InputStream in = null;
            try {
                in = connection.getInputStream();
                responseReader.onResponse(responseCode, new JsonReader(new InputStreamReader(in, "UTF-8")));
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
    }

    public static class Parameters extends HashMap<String,List<String>> {
        public Parameters add(String name, String value) {
            if (!containsKey(name)) {
                put(name, new ArrayList<String>());
            }
            get(name).add(value);
            return this;
        }
    }

    public interface ResponseReader {
        public void onError(int responseCode, InputStream err) throws IOException;
        public void onResponse(int responseCode, JsonReader reader) throws IOException;
    }
}
