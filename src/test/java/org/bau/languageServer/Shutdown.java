package org.bau.languageServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.bau.json.JsonObject;
import org.bau.json.JsopBuilder;

public class Shutdown {
    JsonObject obj;
    JsonObject response;
    String id;

    public Shutdown(JsonObject obj) {
        this.obj = obj;
        this.id = obj.getProperties().get("id");

    }

    public byte[] process() throws IOException {
        JsopBuilder b = new JsopBuilder();
        b.object().
            key("id").encodedValue(id).
            key("jsonrpc").value("2.0").
            key("result").value(null).
        endObject();

        JsonObject response = JsonObject.fromJson(b.toString(), true);
        byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
        System.out.println("Content-Length:" + bytes.length + "\r\n\r\n");
        System.out.write(bytes);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String header = "Content-Length:" + bytes.length + "\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.writeBytes(bytes);
        out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
