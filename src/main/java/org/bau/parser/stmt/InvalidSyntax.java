package org.bau.parser.stmt;

import org.bau.parser.Section;

public class InvalidSyntax implements Section {

    private final String text;

    InvalidSyntax(String text) {
        this.text = text;
    }

    @Override
    public String formatSource() {
        return text;
    }

}
