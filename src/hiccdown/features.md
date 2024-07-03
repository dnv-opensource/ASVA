# Heading 1

Markdown allows various levels of headings, created using `#`.

## Heading 2

Sub-heading under Heading 1, using `##`.

### Heading 3

Further sub-heading, using `###`.

#### Heading 4

Using `####` for smaller sub-headings.

##### Heading 5

`#####` creates even smaller headings.

###### Heading 6

`######` is the smallest heading size in Markdown.

---

### Paragraphs

Paragraphs in Markdown are just plain text separated by blank lines.

This is another paragraph. Markdown ignores single line breaks, so this line will appear right after the previous one in the same paragraph.

---

### Emphasis

_Italic text_ is created using `*` or `_` around the text.

**Bold text** uses `**` or `__`.

**_Bold and italic text_** can be created by using `***` or `___`.

---

### Blockquotes

> Blockquotes are created using `>`.
>
> They can span multiple paragraphs if you continue to use `>` before each line.

---

### Lists

#### Unordered List

- Item 1
- Item 2
  - Sub Item 2.1
  - Sub Item 2.2

Using `-`, `+`, or `*` creates bullet points.

#### Ordered List

1. First Item
2. Second Item
   1. Sub Item 2.1
   2. Sub Item 2.2

Numbers followed by a period `.` or a parenthesis `)` create an ordered list.

---

### Code

#### Inline Code

This is an inline code: `println("Hello World")`

Inline code is created using backticks `` ` ``.

#### Code Blocks

```clojure
;; Clojure code example
(defn hello-world []
  (println "Hello, World!"))
```

Code blocks are created by using three backticks ` ``` ` or tildes `~~~`, optionally followed by a language identifier.

---

### Links

[Markdown Specification](https://daringfireball.net/projects/markdown/ "Markdown Specification")

Links are created using `[link text](URL "title text")`.

---

### Images

![Alt text](https://via.placeholder.com/150)

Images are like links, but with an exclamation mark `!` in front, `![Alt text](Image URL)`.

---

### Tables

| Header 1 | Header 2 | Header 3 |
| -------- | -------- | -------- |
| Row 1    | Data 1.1 | Data 1.2 |
| Row 2    | Data 2.1 | Data 2.2 |

Tables are created using pipes `|` and dashes `-`.

---

### Horizontal Rule

---

Three or more hyphens `---`, asterisks `***`, or underscores `___` create a horizontal rule.

---

### Footnotes

Here's a text with a footnote.[^1]

[^1]: This is the footnote.

Footnotes are created using `[^identifier]` for the reference and `[^identifier]:` for the actual footnote.
