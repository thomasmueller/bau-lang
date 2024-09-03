## Regular Expression Version 2

### Problems

Regular expressions are powerful, flexible, and concise.

However, due to the escaping rules, often regular expressions are hard to read.
Many characters require escaping if one likes to search for them:
`\` `{` `}` `(` `)` `[` `]` `|` `*` `+` `?` `^` `$` `.`.
The escaping rules are different inside square brackets: here, only `\` always need to be escaped,
but sometimes also (depending on the position) `:` `[` `]` `^` and `-`.
The escaping rules are arguably hard to remember and use. It is easy to make mistakes.
Escaping is especially a challenge when the regular expression is embedded in a host language like Java or C.
In this case, often double escaping is needed, which becomes even harder to read.
As an example, the regular expression for a decimal number, `^\d*\.\d+$`, 
requires double escaping when embedded into the Java host language: 
`s.matches("\\d*\\.\\d");`. Searching for "not a backslash" is `"[^\\\\]"`.

A second problem is that regular expressions can be hard to read because
(a) there are many single-character abbreviations, and (b) due to lack of spacing.
Some regular expression flavors support character classes such as `:digit:`, `:lower:`, `\p{Lower}`.
For (b) there is no solution.

### Regular Expression Version 2 Syntax

Escaping can almost completely be eliminated using a sligtly different syntax.
In short, literals are quoted as in SQL, and escaping backslash are removed.
This also allows using spaces to improve readability.


| RegEx V2         | Matches                | RegEx     |
|------------------|------------------------|-----------|
| `'text'`         | Exactly `text`         | `text`    | 
| `'x''y'`         | Exactly `x'y`          | `x'y`     |
| `['ab']`         | Character `a` or `b`   | `[ab]`    |
| `['0-9_']`       | Digit, or `_`          | `[0-9_]`  |
| `['0-9a-f']`     | Hex digit              | `[0-9a-f]`|
| `[^'a']`         | Not the character `a`  | `[^a]`    |
| `('19'\|'20')`   | One or the other       | `(19\|20)`|
| `d`              | A digit (`0`-`9`)      | `\d`      |
| `b`              | A word boundary        | `\b`      |
| `s`              | A whitespace character | `\s`      |
| `t`              | Tab character          | `\t`      |
| `n`              | Newline                | `\n`      |
| `w`              | Word character         | `\w`      |
| `^`              | Beginning of the text  | `^`       |
| `$`              | End of text            | `$`       |
| `.`              | Any character          | `.`       |
| `d?`             | Zero or one digit      | `\d?`     |
| `d+`             | One or more digits     | `\d+`     |
| `d*`             | Any number of digits   | `\d*`     |
| `d{4}`           | Exactly 4 digits       | `\d{4}`   |

Examples:

| RegEx V2                          | Matches                            | RegEx                                    |
|-----------------------------------|------------------------------------|------------------------------------------|
| `^d*('.'d+)?$`                    | Decimal number                     | `^\d*(\.\d+)?$`                          |
| `^['-+']?d+$`                     | Positive or negative number        | `^[-+]?\d+$`                             |
| `^dddd'-'dd'-'dd$`                | Date                               | `^\d{4}-\d{2}-\d{2}$`                    |
| `^dddd'-'dd'-'dd'T'dd':'dd':'dd$` | Timestamp                          | `^\d{4}-\d{2}-\d{2}T$\d{2}:\d{2}:\d{2}$` |
| `^[' 't]+\|[' 't]+$`              | Whitespace at the beginning or end | `^[ \t]+\|[ \t]+$`                       |
| `[^x00-x7f]`                      | Find non-ASCII characters          | `[^\x00-\x7F]`                           |
| `b'variable'b`                    | Find a variable                    | `\bvariable\b`                           | 

## Comparison

In most cases, the version 2 syntax is only slightly longer than the original.
There are many cases where version 2 is shorter, for example to match decimal numbers
and timestamps (see above). 
In some cases, version 2 is shoter; 
in many cases it is a bit longer, but arguably easier to read.
This is specially the case when the expression is embedded in a host language such as Java,
where backslashes need to be escaped. As an example:

    timestampV1 = "^\\d{4}-\\d{2}-\\d{2}T$\\d{2}:\\d{2}:\\d{2}$";
    timestampV2 = "^dddd'-'dd'-'dd'T'dd':'dd':'dd$";
    
Version 2 allows to use spaces for grouping, to make the expression better readble. 
Existing regular expression syntax do not have this ability:

    timestampV2 = "^ dddd'-'dd'-'dd 'T' dd':'dd':'dd $";

## Related Work

Many regular expression implementations already support a way to quote a literal, by enclosing the literal between `\Q` and `\E`. 
This mechanism increases the length of the expression. Also, there are special rules if the terms `\Q` or `\E` are quoted:

## Migration

Switching to the version 2 syntax is very easy, because
the operators and abbreviations are the same as in the existing syntax:
only quotes have to be added, and backslashes have to be removed.
People that are familar with the exsting syntax will have no problem
with the change.

If existing regular expression libraries want to support both 
the old as well as the new version 2 syntax, the prefix `(?2)` can be used.
This is compatible with the existing flag syntax; 
eg. `(?i)` enabled the case insensitive mode in many implementations.

