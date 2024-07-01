package org.bau;

/*

Multi-line strings, Raw string
https://xkcd.com/1638/ Backslashes

Python      ''' or """   (\n is newline)
            r'ab\n'      (raw string)
Ruby        ''' or """
Javascript  `
C#          @"  ... "" ... "
Scala       """  and |
Swift       """  but \ means _no_ line break (multi-line)
            #"\Hello \World"#                (raw string)
            ####"This is a string"####
Rust        r#"  ... "#
            r###" ... "###
            " ... \ continue ... \ "
C++         "aa" "bb" adjacent string literals
            macro MULTI_LINE_STRING
            C++11 raw strings
Go          backtick ` escape sequences are not interpreted


raw multi-line strings indentation: function to remove indentation
https://sarunw.com/posts/swift-multiline-string/#:~:text=With%20different%20indentations%2C%20a%20multiline,closing%20quotation%20marks%20are%20ignored.

Swift:
- multi-line string must start on a new line; close must start on a new line
- Use a backslash (\) to omits the following line break.

Golang `
"It is therefor the string literal of choice to write regular expression patterns as they usually 
contain non-standard escape sequences that would make the Go compiler complain of not double-escaped. 
It keeps the patterns clean and relatively readable."


What about: 
- ` (at least one, but as many as you want) starts and ends a raw string
- raw strings can span multiple lines.
- if there are only spaces on the same line, then the text start on the next line, and indentation is removed up to the end of the string

- """ at least 3, but as many double quotes as you like
- ends in the same way as it begun
- if there is a newline afterwards, then the string starts on the next line.
  in this case, the last line (where it ends) defines the identation.
  in this case, so many spaces are removed in each line.
- escape sequences are not interpreted.

a = """abc\n""" # raw string
a = """
  This is a two-line multiline raw string.
  This is the second line. This is backslash n: \n it is ignored: C:\data
  """




 */
public class LanguageStrings {

}
