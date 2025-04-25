package org.bau.languageServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.bau.stdlib.json.Json;
import org.bau.stdlib.json.JsonBuilder;

/*

/*


{
  "jsonrpc": "2.0",
  "id": "1",
  "method": "initialize",
  "params": {
    "processId": 2543,
    "rootPath": "/Users/mueller/data/bau/",
    "rootUri": "file:///Users/mueller/data/bau/",
    "capabilities": {
      "workspace": {
        "applyEdit": true,
        "workspaceEdit": {
          "documentChanges": true,
          "resourceOperations": [
            "create",
            "delete",
            "rename"
          ],
          "failureHandling": "undo"
        },
        "symbol": {
          "dynamicRegistration": true
        },
        "executeCommand": {
          "dynamicRegistration": true
        },
        "workspaceFolders": true
      },
      "textDocument": {
        "synchronization": {
          "willSave": true,
          "willSaveWaitUntil": true,
          "didSave": true
        },
        "completion": {
          "completionItem": {
            "snippetSupport": true,
            "documentationFormat": [
              "markdown",
              "plaintext"
            ]
          }
        },
        "hover": {
          "contentFormat": [
            "markdown",
            "plaintext"
          ]
        },
        "signatureHelp": {},
        "references": {},
        "documentHighlight": {},
        "documentSymbol": {
          "symbolKind": {
            "valueSet": [
              18,
              17,
              5,
              14,
              9,
              10,
              22,
              24,
              8,
              1,
              12,
              11,
              20,
              6,
              2,
              3,
              21,
              16,
              19,
              25,
              4,
              7,
              15,
              23,
              26,
              13
            ]
          },
          "hierarchicalDocumentSymbolSupport": true
        },
        "formatting": {
          "dynamicRegistration": true
        },
        "rangeFormatting": {},
        "definition": {
          "linkSupport": true
        },
        "typeDefinition": {
          "linkSupport": true
        },
        "codeAction": {
          "codeActionLiteralSupport": {
            "codeActionKind": {
              "valueSet": [
                "quickfix",
                "refactor",
                "refactor.extract",
                "refactor.inline",
                "refactor.rewrite",
                "source",
                "source.organizeImports"
              ]
            }
          },
          "dynamicRegistration": true
        },
        "codeLens": {},
        "documentLink": {},
        "colorProvider": {},
        "rename": {},
        "foldingRange": {}
      }
    },
    "clientName": "Eclipse SDK",
    "trace": "off",
    "workspaceFolders": [
      {
        "uri": "file:/Users/mueller/data/KVStore",
        "name": "KVStore"
      },
      {
        "uri": "file:/Users/mueller/data/bau",
        "name": "bau"
      },
      {
        "uri": "file:/Users/mueller/data/filtersCode/fastfilter_java/fastfilter",
        "name": "fastfilter"
      },
      {
        "uri": "file:/Users/mueller/data/h2database/h2",
        "name": "h2database"
      },
      {
        "uri": "file:/Users/mueller/jackrabbit-oak/oak-api",
        "name": "oak-api"
      },
      {
        "uri": "file:/Users/mueller/jackrabbit-oak/oak-commons",
        "name": "oak-commons"
      },
      {
        "uri": "file:/Users/mueller/jackrabbit-oak/oak-core",
        "name": "oak-core"
      },
      {
        "uri": "file:/Users/mueller/jackrabbit-oak/oak-core-spi",
        "name": "oak-core-spi"
      },
      {
        "uri": "file:/Users/mueller/jackrabbit-oak/oak-query-spi",
        "name": "oak-query-spi"
      },
      {
        "uri": "file:/Users/mueller/jackrabbit-oak/oak-store-spi",
        "name": "oak-store-spi"
      },
      {
        "uri": "file:/Users/mueller/OneDrive%20-%20Adobe/test",
        "name": "test"
      },
      {
        "uri": "file:/Users/mueller/adobe/mueller/utilities",
        "name": "utilities"
      }
    ]
  }
}


https://medium.com/@malintha1996/understanding-the-language-server-protocol-5c0ba3ac83d2

 */

public class Initialize {

    Json obj;
    Json response;
    String id;

    public Initialize(Json obj) {
        this.obj = obj;
        this.id = obj.get("id").getString();

    }

    public byte[] process() throws IOException {
        JsonBuilder b = new JsonBuilder();
        b.object().
            key("id").encodedValue(id).
            key("jsonrpc").value("2.0").
            key("result").object().
                key("capabilities").object().
                    key("completionProvider").object().
                        key("triggerCharacters").array().
                            value(".").
                        endArray().
                    endObject().
                    key("signatureHelpProvider").object().
                        key("triggerCharacters").array().
                            value("(").
                            value(",").
                        endArray().
                    endObject().
                endObject().
            endObject().
        endObject();

        String response = b.toString();

        byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String header = "Content-Length:" + bytes.length + "\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.writeBytes(bytes);
        out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();

//        response = new JsonObject();
//        response.getProperties().put("id", id);
//        response.getProperties().put("jsonrpc", "\"2.0\"");
//        JsonObject result = new JsonObject();
//        response.getChildren().put("result", result);
//        JsonObject capabilities = new JsonObject();
//        result.getChildren().put("capabilities", capabilities);
//        JsonObject completionProvider = new JsonObject();
//        capabilities.getChildren().put("completionProvider", completionProvider);
//        completionProvider
//
//        "triggerCharacters": [
//                              ":",
//                              ".",
//                              "\u003e",
//                              "@"
//                            ]
//                          },
//                          "signatureHelpProvider": {
//                            "triggerCharacters": [
//                              "(",
//                              ","
//                            ]
//                          },
//
//
//        byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
//        System.out.println("Content-Length:" + bytes.length + "\r\n\r\n");
//        System.out.writeBytes(bytes);
//        return "";
    }

}
