package org.bau.languageServer;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.bau.stdlib.json.Json;

public class BauLanguageServer {

	// https://stackoverflow.com/questions/30687783/create-custom-language-in-visual-studio-code

	// https://macromates.com/manual/en/language_grammars
	// https://code.visualstudio.com/api/language-extensions/language-configuration-guide
	// https://github.com/microsoft/vscode/tree/main/extensions

    // https://langserver.org/

    // https://code.visualstudio.com/api/language-extensions/syntax-highlight-guide
    // https://github.com/worlpaker/go-syntax/blob/master/syntaxes/go.tmLanguage.json
	// https://github.com/microsoft/vscode/blob/main/extensions/lua/syntaxes/lua.tmLanguage.json

    final static String stdInFile = "langServ.in.txt";
    final static String stdOutFile = "langServ.out.txt";

    public static void main(String[] args) throws IOException {
        OutputStream logOut = new FileOutputStream(stdOutFile);
        OutputStream logIn = new FileOutputStream(stdInFile);
        try {
            run(logOut, logIn);
        } finally {
            logIn.close();
            logOut.close();
        }
    }

    private static void run(OutputStream logOut, OutputStream logIn) throws IOException {
        BufferedInputStream in = new BufferedInputStream(System.in);
        int nextContentLength = 0;
        while(true) {
            byte[] data = new byte[4 * 1024];
            int i = 0;
            for(;; i++) {
                int x = in.read();
                if (x == -1) {
                    return;
                }
                data[i] = (byte)x;
                if (x == '\n') {
                    break;
                }
            }
            logIn.write(data, 0, i);
            String line = new String(data, 0, i, StandardCharsets.UTF_8);

            line = line.trim();
            if (line.startsWith("Content-Length:")) {
                nextContentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
            } else if (line.length() == 0) {
                 byte[] data2 = in.readNBytes(nextContentLength);
                 logIn.write(data2);
                 String content = new String(data2, StandardCharsets.UTF_8).trim();
                 Json obj = new Json(content);
                 logIn.write(obj.toString().getBytes(StandardCharsets.UTF_8));
                 String method = obj.get("method").getString();
                 if (method.equals("\"initialize\"")) {
                     byte[] result = new Initialize(obj).process();
                     System.out.write(result);
                     logOut.write(result);
                 } else if (method.equals("\"initialized\"")) {
                     // ignore
                 } else if (method.equals("\"$/setTrace\"")) {
                     // ignore
                 } else if (method.equals("\"$/logTrace\"")) {
                     // ignore
                 } else if (method.equals("\"shutdown\"")) {
                     byte[] result = new Shutdown(obj).process();
                     System.out.write(result);
                     logOut.write(result);
                     // System.out.println(new Initialize(obj).process());
                 } else if (method.equals("\"textDocument/codeAction\"")) {
                     System.out.println(content);
                     // System.out.println(new Initialize(obj).process());
                 } else if (method.equals("\"textDocument/formatting\"")) {
                     System.out.println(content);
                     // System.out.println(new Initialize(obj).process());
                 } else if (method.equals("\"exit\"")) {
                     System.out.println(content);
                     break;
                     // System.out.println(new Initialize(obj).process());
                 } else {
                     System.out.println("Unknown method " + method);
                 }
            }
            logOut.flush();
            System.out.flush();
        }
    }

}
