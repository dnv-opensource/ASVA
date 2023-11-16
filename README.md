# ASVA

This web application provides an elegant interface for displaying ASVA JSON
files, a format developed by OWASP for documenting the security aspects of a
system. These files serve as a comprehensive checklist, detailing essential
security measures, protocols etc. The application enhances the usability of
these files by offering search and filter capabilities, facilitating a
deeper understanding and easier management of system security data.

## Requirements

To set up and run this project, you will need:

- **Clojure**: The project is built with Clojure and ClojureScript. [Install Clojure](https://clojure.org/guides/getting_started) on your system.
- **Node.js and npm**: Required for managing JavaScript dependencies and executing compiled ClojureScript. [Download Node.js](https://nodejs.org/).
- **Shadow-cljs**: Used for ClojureScript compilation. Install it via npm: `npm install -g shadow-cljs`.
- **Git**: Required for fetching certain dependencies. [Download Git](https://git-scm.com/downloads).

## Usage

```sh
git clone https://github.com/dnv-opensource/ASVA
cd ASVA
clojure -M:dev
```

This will clone the repository and run a web-server using shadow-cljs. Just
follow the on-screen instructions to access and interact with the application.
The application typically runs on http://localhost:8080/.
