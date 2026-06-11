(ns hiccdown.core
  (:require [hiccdown.emit :as emit]
            [hiccdown.hiccup :as hiccup]
            [hiccdown.parse :as parse]))

(defn md->ir
  ([markdown-string]
   (md->ir markdown-string {}))
  ([markdown-string opts]
   (parse/md->ir markdown-string opts)))

(defn ir->hiccup
  ([ir]
   (ir->hiccup ir {}))
  ([ir opts]
   (hiccup/ir->hiccup ir opts)))

(defn md->hiccup
  ([markdown-string]
   (md->hiccup markdown-string {}))
  ([markdown-string opts]
   (ir->hiccup (md->ir markdown-string opts) opts)))

(defn hiccup->ir
  ([hiccup]
   (hiccup->ir hiccup {}))
  ([hiccup opts]
   (hiccup/hiccup->ir hiccup opts)))

(defn ir->md
  ([ir]
   (ir->md ir {}))
  ([ir opts]
   (emit/ir->md ir opts)))

(defn hiccup->md
  ([hiccup]
   (hiccup->md hiccup {}))
  ([hiccup opts]
   (ir->md (hiccup->ir hiccup opts) opts)))
