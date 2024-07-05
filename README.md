# ASVA

ASVA offers an interface for interacting with ASVS JSON files, a
format developed by OWASP for documenting the security aspects of a
system.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Live Demo](https://img.shields.io/badge/demo-online-green.svg)](https://dnv-opensource.github.io/ASVA/)

## Requirements

To set up and run this project locally, you'll need:

- **Clojure**: The project is built with Clojure and ClojureScript. [Install Clojure](https://clojure.org/guides/getting_started) on your system.
- **Node.js and npm**: Required for managing JavaScript dependencies and executing compiled ClojureScript. [Download Node.js](https://nodejs.org/).

## Installation

```sh
git clone https://github.com/dnv-opensource/asva
cd asva
npm install
```

## Usage

Run the following command to start the web server using shadow-cljs:

```sh
clojure -M:dev
```

Follow the on-screen instructions to access and interact with the application,
typically running on http://localhost:8080/.

On the initial page, you will be prompted to select a JSON file from
the available options, which can be found in multiple languages at
[this GitHub repository](https://github.com/OWASP/ASVS/tree/v4.0.3/4.0). You can choose between a detailed version,
which includes an introductory section that offers additional context
and may be customized for your project, and a flat version. Once
selected, you will access the main application interface. Here, you
can either import an existing ASVA.json file, if you have one, or make
adjustments and export a new ASVA.json file. All data is securely
stored in your browser's local storage.

## License

This project is licensed under the MIT License - see the [LICENSE.md](./LICENSE.md) file for details.
