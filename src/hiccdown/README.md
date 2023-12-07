# Hiccdown

Hiccdown is a versatile and efficient Clojure/ClojureScript library designed for
bi-directional conversion between Markdown text and Hiccup format. Ideal for web
applications using ClojureScript and Reagent or server-side applications in
Clojure, Hiccdown facilitates seamless integration and transformation of content
between these two popular formats.

## Features

- **Markdown to Hiccup and Vice Versa**: Converts Markdown to Hiccup and Hiccup back to Markdown.
- **Clojure/ClojureScript Compatibility**: Seamlessly works in both Clojure and ClojureScript environments.
- **Reagent Integration**: Easily integrates with Reagent projects, enhancing React component rendering with Markdown and Hiccup content.
- **Potential for Extensibility**: While currently not extensible, future updates may include extensible features based on user feedback and adoption.

## Installation

```clojure
{net.clojars.simtech/hiccdown {:mvn/version "0.1.0"}}
```

## Usage

### Markdown to Hiccup

```clojure
(require '[hiccdown.core :as hd])

(def hiccup-content
 (hd/markdown->hiccup "Your *Markdown* text here."))
```

### Hiccup to Markdown

```clojure
(def markdown-content
 (hd/hiccup->markdown [:p "Your " [:em "Hiccup"] " content here."]))
```

## Contributing

Contributions to Hiccdown are welcome and appreciated. Your input can help with
adding new features, fixing bugs, or improving the library's documentation.

## License

Distributed under the MIT License. See LICENSE for more information.
