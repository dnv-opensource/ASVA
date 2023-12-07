# ASVA

This web application offers a sophisticated interface for interacting with ASVS
JSON files, a format developed by OWASP for documenting the security aspects of
a system. It not only displays these files as a comprehensive checklist
detailing essential security measures and protocols but also enables users to
add personalized notes to each item. This interactivity enhances the management
and documentation of security measures, facilitating a deeper understanding and
more effective tracking of system security data.

## Requirements

To set up and run this project, you will need:

- **Clojure**: The project is built with Clojure and ClojureScript. [Install Clojure](https://clojure.org/guides/getting_started) on your system.
- **Node.js and npm**: Required for managing JavaScript dependencies and executing compiled ClojureScript. [Download Node.js](https://nodejs.org/).
- **Shadow-cljs**: Used for ClojureScript compilation. Install it via npm: `npm install -g shadow-cljs`.

## Installation

```sh
git clone https://github.com/dnv-opensource/ASVA
cd ASVA
```

## Usage

Run the following command to start the web server using shadow-cljs:

```sh
clojure -M:dev
```

Follow the on-screen instructions to access and interact with the application,
typically running on http://localhost:8080/.

On the initial page, you will be prompted to select a JSON file. Supported files
are available at https://github.com/OWASP/ASVS/tree/v4.0.3/4.0/ in multiple
languages, both a detailed version and a flat version. The key difference is
that the detailed version includes an introduction, which can provide additional
context and insights.
