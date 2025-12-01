package org.bau.languageServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.bau.stdlib.json.JsonReader;
import org.bau.stdlib.json.JsonBuilder;

public class Shutdown {
    JsonReader obj;
    JsonReader response;
    String id;

    public Shutdown(JsonReader obj) {
        this.obj = obj;
        this.id = obj.get("id").getString();
    }

    public byte[] process() throws IOException {
        JsonBuilder b = new JsonBuilder();
        b.object().
            key("id").encodedValue(id).
            key("jsonrpc").value("2.0").
            key("result").value(null).
        endObject();

        String response = b.toString();
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
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
