package org.bau.languageServer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.bau.parser.Parser;
import org.bau.parser.Program;
import org.bau.stdlib.json.JsonBuilder;
import org.bau.stdlib.json.JsonReader;

public class BauLanguageServer {

    // TODO:
    // - format (pretty print)
    // - compile time errors
    // - recompile if changed
    // - autocomplete after a dot
    // - goto definition
    // - find references
    // - hover info
    // - rename a variable / function
    // - formatting

    // https://code.visualstudio.com/api/language-extensions/language-configuration-guide
    // https://code.visualstudio.com/api/language-extensions/syntax-highlight-guide
    // https://github.com/microsoft/vscode/blob/main/extensions/lua/syntaxes/lua.tmLanguage.json
    // https://github.com/microsoft/vscode/tree/main/extensions
    // https://github.com/tower-lsp-community/tower-lsp-server/tree/main
    // https://github.com/worlpaker/go-syntax/blob/master/syntaxes/go.tmLanguage.json
    // https://langserver.org/
    // https://macromates.com/manual/en/language_grammars
    // https://medium.com/@malintha1996/understanding-the-language-server-protocol-5c0ba3ac83d2
	// https://stackoverflow.com/questions/30687783/create-custom-language-in-visual-studio-code

    final static String logFile = "langServ.log.txt";

    private HashMap<String, String> uriToModule = new HashMap<>();
    private HashMap<String, String> moduleSource = new HashMap<>();
    private HashMap<String, Program> moduleProgram = new HashMap<>();
    private boolean html;

    public static void main(String[] args) throws IOException {
        new BauLanguageServer().run(args);
    }

    private void run(String[] args) throws IOException {
        OutputStream logOut;
        for (int i = 0;; i++) {
            String f = i == 0 ? logFile : logFile + i + ".txt";
            if (!new File(f).exists()) {
                logOut = new FileOutputStream(f);
                break;
            }
        }
        try {
            run(logOut);
        } finally {
            logOut.close();
        }
    }

    private void run(OutputStream logOut) throws IOException {
        BufferedInputStream in = new BufferedInputStream(System.in);
        int nextContentLength = 0;
        while (true) {
            byte[] data = new byte[4 * 1024];
            int i = 0;
            for (;; i++) {
                int x = in.read();
                if (x == -1) {
                    return;
                }
                data[i] = (byte) x;
                if (x == '\n') {
                    break;
                }
            }
            logOut.write("=== << ===\n".getBytes(StandardCharsets.UTF_8));
            logOut.write(data, 0, i);
            logOut.write("=== >> ===\n".getBytes(StandardCharsets.UTF_8));
            String line = new String(data, 0, i, StandardCharsets.UTF_8);
            line = line.trim();
            if (line.startsWith("Content-Length:")) {
                nextContentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
            } else if (line.length() == 0) {
                byte[] data2 = in.readNBytes(nextContentLength);
                logOut.write(data2);
                logOut.write("\n".getBytes(StandardCharsets.UTF_8));
                String content = new String(data2, StandardCharsets.UTF_8).trim();
                try {
                    boolean exit = process(logOut, content);
                    if (exit) {
                        break;
                    }
                } catch (Exception t) {
                    throw new RuntimeException("Error: " + content, t);
                }
            }
            logOut.flush();
            System.out.flush();
        }
    }

    private boolean process(OutputStream logOut, String content) throws IOException {
        JsonReader obj = new JsonReader(content);
        String method = obj.get("method").getString();
        JsonReader idReader = obj.get("id");
        String id = idReader == null ? "" : idReader.getString();
        if (method.equals("exit")) {
            return true;
        } else if (method.equals("initialize")) {

            JsonReader params = obj.get("params");
            if (params != null) {
                JsonReader clientInfo = params.get("clientInfo");
                if (clientInfo != null) {
                    String clientName = clientInfo.getString("name");
                    // String clientVersion = clientInfo.getString("version");
                    if ("Eclipse IDE".equals(clientName)) {
                        html = true;
                    }
                }
            }
            JsonBuilder b = new JsonBuilder();
            b.object()
                .key("id").encodedValue(id)
                .key("jsonrpc").value("2.0")
                .key("result").object()
                    .key("capabilities").object()
                        .key("completionProvider").object()
                            .key("triggerCharacters").array()
                                .value(".")
                            .endArray()
                        .endObject()
                        .key("definitionProvider").value(true).
                        // documentFormattingProvider
                        //   "semanticTokensProvider": {
                        //       "legend": {
                        //         "tokenTypes": [
                        //           "namespace", "type", "class", "enum", "interface",
                        //           "struct", "typeParameter", "function", "method",
                        //           "property", "variable", "parameter", "keyword",
                        //           "comment", "string", "number", "operator"
                        //         ],
                        //         "tokenModifiers": [
                        //           "declaration", "definition", "readonly", "static",
                        //           "deprecated", "abstract", "modification", "documentation"
                        //         ]
                        //       },
                        //       "full": true
                        //     }
                        key("hoverProvider").value(true).
                        key("textDocumentSync").value(1).
                        key("referencesProvider").value(true).
                        key("signatureHelpProvider").object().
                            key("triggerCharacters").array().
                                value("(").
                                value(",").
                            endArray().
                        endObject().
                    endObject().
                endObject().
            endObject();
            byte[] result = BauLanguageServer.convert(b);
            System.out.write(result);
            logOut.write(result);
        } else if (method.equals("initialized")) {
            // ignore
        } else if (method.equals("$/setTrace")) {
            // ignore
        } else if (method.equals("$/logTrace")) {
            // ignore
        } else if (method.equals("$/cancelRequest")) {
            // ignore
        } else if (method.equals("shutdown")) {
            JsonBuilder b = new JsonBuilder();
            b.object().
                key("id").encodedValue(id).
                key("jsonrpc").value("2.0").
                key("result").value(null).
            endObject();
            byte[] result = BauLanguageServer.convert(b);
            System.out.write(result);
            logOut.write(result);
        } else if (method.equals("textDocument/codeAction")) {
            // [] => no actions
            JsonBuilder b = new JsonBuilder();
            b.object().
                key("id").encodedValue(id).
                key("jsonrpc").value("2.0").
                key("result").array().endArray().
            endObject();
            byte[] result = BauLanguageServer.convert(b);
            System.out.write(result);
            logOut.write(result);
            // LSP uses 0-based line and character numbers
            // {
            //   "id": 1,
            //   "jsonrpc": "2.0",
            //   "result": [
            //     {
            //       "title": "Import 'io'",
            //       "kind": "quickfix",
            //       "edit": {
            //         "changes": {
            //           "file:///path/to/file.bau": [
            //             {
            //               "range": {
            //                 "start": {"line": 0, "character": 0},
            //                 "end": {"line": 0, "character": 0}
            //               },
            //               "newText": "import io\n"
            //             }
            //           ]
            //         }
            //       }
            //     }
            //   ]
            // }
            // Or "result": [] for no actions. kind values: "quickfix", "refactor", "source".
        } else if (method.equals("textDocument/completion")) {
            // [] = "no completions"
            JsonBuilder b = new JsonBuilder();
            b.object().
                key("id").encodedValue(id).
                key("jsonrpc").value("2.0").
                key("result").array().endArray().
            endObject();
            byte[] result = BauLanguageServer.convert(b);
            System.out.write(result);
            logOut.write(result);
            // {
            //     "id": "2",
            //     "jsonrpc": "2.0",
            //     "method": "textDocument/completion",
            //     "params": {
            //         "context": {
            //             "triggerKind": 1
            //         },
            //         "textDocument": {
            //             "uri": "file:///Users/mueller/data/bau/demo.bau"
            //         },
            //         "position": {
            //             "character": 5,
            //             "line": 29
            //         }
            //     }
            // }
            // {
            //   "id": 1,
            //   "jsonrpc": "2.0",
            //   "result": [
            //     {
            //       "label": "add",
            //       "kind": 2,
            //       "detail": "fun List.add(item int)",
            //       "documentation": "Adds an item to the list"
            //     }
            //   ]
            // }
            // kind values: 1=text, 2=method, 3=function, 6=variable, 7=class/type. Or "result": [] for no completions.
        } else if (method.equals("textDocument/definition")) {
            // expects null or a location object, not []. An empty array may confuse some clients.
            JsonBuilder b = new JsonBuilder();
            b.object().
                key("id").encodedValue(id).
                key("jsonrpc").value("2.0").
                key("result").value(null).
            endObject();
            byte[] result = BauLanguageServer.convert(b);
            System.out.write(result);
            logOut.write(result);
            // {
            //   "id": 1,
            //   "jsonrpc": "2.0",
            //   "result": {
            //     "uri": "file:///path/to/file.bau",
            //     "range": {
            //       "start": {"line": 5, "character": 4},
            //       "end": {"line": 5, "character": 7}
            //     }
            //   }
            // }
            // Or "result": null if nothing found at that position
            // requires a response
        } else if (method.equals("textDocument/didClose")) {
        } else if (method.equals("textDocument/didChange")) {
            // eg. params - textDocument - text = file content; uri = file:///...
            // no response needed
            // Severity 1 = error, 2 = warning, 3 = information, 4 = hint.
            // could reply with:
            // {
            //     "jsonrpc": "2.0",
            //     "method": "textDocument/publishDiagnostics",
            //     "params": {
            //       "uri": "file:///path/to/file.bau",
            //       "diagnostics": [
            //         {
            //           "range": {
            //             "start": {"line": 5, "column": 3},
            //             "end": {"line": 5, "column": 8}
            //           },
            //           "severity": 1,
            //           "message": "Unknown type 'Foo'"
            //         }
            //       ]
            //     }
            //   }
            // no response needed
        } else if (method.equals("textDocument/didOpen")) {
            // no response needed
            // {
            //     "jsonrpc": "2.0",
            //     "method": "textDocument/didOpen",
            //     "params": {
            //         "textDocument": {
            //             "languageId": "bau",
            //             "text": "..."
            //             "uri": "file:///Users/mueller/data/bau/demo.bau",
            //             "version": 1
            //         }
            //     }
            // }
            JsonReader params = obj.get("params");
            if (params != null) {
                JsonReader textDocument = params.get("textDocument");
                if (textDocument != null) {
                    String text = textDocument.getString("text");
                    String uri = textDocument.getString("uri");
                    if (uri != null && uri.endsWith(".bau") && text != null) {
                        compile(uri, text);
                    }
                }
            }
        } else if (method.equals("textDocument/didSave")) {
            // no response needed
        } else if (method.equals("textDocument/formatting")) {
            // [] is correct for "no changes"
            // null for "not available right now".
            // {
            //     "id": 5,
            //     "jsonrpc": "2.0",
            //     "method": "textDocument/formatting",
            //     "params": {
            //       "textDocument": {
            //         "uri": "file:///path/to/file.bau"
            //       },
            //       "options": {
            //         "tabSize": 4,
            //         "insertSpaces": true
            //       }
            //     }
            //   }
            // {
            //     "id": 5,
            //     "jsonrpc": "2.0",
            //     "result": [
            //       {
            //         "range": {
            //           "start": {"line": 0, "character": 0},
            //           "end": {"line": 999999, "character": 0}
            //         },
            //         "newText": "...formatted source code..."
            //       }
            //     ]
            //   }
            JsonBuilder b = new JsonBuilder();
            b.object().
                key("id").encodedValue(id).
                key("jsonrpc").value("2.0").
                key("result").array().endArray().
            endObject();
            byte[] result = BauLanguageServer.convert(b);
            System.out.write(result);
            logOut.write(result);
            //     "result": [
            //       {
            //         "range": {
            //           "start": {"line": 0, "character": 0},
            //           "end": {"line": 100, "character": 0}
            //         },
            //         "newText": "...formatted source..."
            //       }
            //     ]
            //   }
            //   A list of text edits to apply.
            //   Typically one edit covering the whole file.
            //   Or "result": null if no changes needed.
        } else if (method.equals("textDocument/hover")) {
            JsonReader params = obj.get("params");
            String hover = null;
            if (params != null) {
                JsonReader textDocument = params.get("textDocument");
                JsonReader position = params.get("position");
                if (textDocument != null && position != null) {
                    String uri = textDocument.getString("uri");
                    long character = position.getLong("character");
                    long line = position.getLong("line");
                    if (uri != null && uri.endsWith(".bau")) {
                        hover = hover(uri, line, character);
                    }
                }
            }
            //     "method": "textDocument/hover",
            //     "params": {
            //         "textDocument": {
            //             "uri": "file:///Users/mueller/data/bau/demo.bau"
            //         },
            //         "position": {
            //             "character": 25,
            //             "line": 16
            //         }
            JsonBuilder b = new JsonBuilder();
            if (hover == null) {
                b.object().
                    key("id").encodedValue(id).
                    key("jsonrpc").value("2.0").
                    key("result").value(null).
                    endObject();
            } else {
                b.object().
                    key("id").encodedValue(id).
                    key("jsonrpc").value("2.0").
                    key("result").object().
                        key("contents").object().
                            key("kind").value("markdown").
                            key("value").value(hover).
                        endObject().
                    endObject().
                endObject();
            }
            byte[] result = BauLanguageServer.convert(b);
            System.out.write(result);
            logOut.write(result);
        } else {
            logOut.write(("\nUnknown method: <" + method + ">\n").getBytes(StandardCharsets.UTF_8));
        }
        return false;
    }

    private String hover(String uri, long line, long character) {
        Program program = getProgram(uri);
        if (program == null) {
            return null;
        }
        String module = uriToModule.get(uri);
        return program.getHover(module, (int) line, (int) character, html);
    }

    private void compile(String uri, String text) {
        // TODO module is no longer in the file
        int moduleStart = text.indexOf("module ");
        String module = "";
        if (moduleStart >= 0) {
            int moduleEnd = text.indexOf("\n", moduleStart);
            if (moduleEnd < 0) {
                moduleEnd = text.length();
            }
            module = text.substring(moduleStart, moduleEnd).trim();
        }
        uriToModule.put(uri, module);
        moduleSource.put(module, text);
        if (module.equals("")) {
            Parser p = new Parser(moduleSource, text);
            try {
                Program prog = p.parse();
                moduleProgram.put(module, prog);
            } catch (Exception e) {
                moduleProgram.remove(module);
            }
        }
    }

    private Program getProgram(String uri) {
        String module = uriToModule.get(uri);
        if (module == null) {
            return null;
        }
        return moduleProgram.get(module);
    }

    public static byte[] convert(JsonBuilder b) {
        String response = b.toString();
        byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String header = "Content-Length: " + bytes.length + "\r\n\r\n";
        try {
            out.write(header.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        out.writeBytes(bytes);
        out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

}
