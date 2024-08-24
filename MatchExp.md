## MatchExp

| MatchExp         | Matches                | RegEx     |
|------------------|------------------------|-----------|
| `begin`          | Beginning of the text  | `^`       |
| `end`            | End of text            | `$`       |
| `'text'`         | Exactly `text`         | `text`    | 
| `any`            | Any character          | `.`       |
| `space`          | A space character      | `\s`      |
| `tab`            | Tab character          | `\t`      |
| `newline`        | Newline                | `\n`      |
| `digit`          | Digit (`0`-`9`)        | `\d`      |
| `word`           | Word character         | `\w`      |
| `newline`        | Newline                | `\n`      |
| `[a, b]`         | Character `a` or `b`   | `[ab]`    |
| `[0-9, _]`       | Digit, or `_`          | `[0-9_]`  |
| `[not a]`        | Not the character `a`  | `[^a]`    |
| `('19' or '20')` | One or the other       | `(19\|20)`|
| `digit?`         | Zero or one digit      | `\d?`     |
| `digit+`         | One or more digits     | `\d+`     |
| `digit*`         | Any number of digits   | `\d*`     |
| `digit * 4`      | Exactly 4 digits       | `\d{4}`   |
| `digit * 4..6`   | 4, 5, or 6 digits      | `\d{4,6}` |

Examples:

| MatchExp                         | Matches                            | RegEx                              |
|----------------------------------|------------------------------------|------------------------------------|
| `[+, -, *, /]`                   | A math operation: +, *, -, /       | `\+\|-\|\*\|/`                     |
| `begin ('-' or '+')? digit+ end` | Positive or negative numbers       | `^(\+\|\-)?\d+$`                   |
| `begin digit+ ('.' digit*)? end` | Decimal number                     | `^\d*(\.\d+)?$`                    |
| `'0x' [0-9, a-f]*`               | Hexadecimal number                 | `0x[0-9a-f]*`                      |
