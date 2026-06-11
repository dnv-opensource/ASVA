(ns hiccdown.error)

(defn hiccdown-ex
  ([type phase reason message]
   (hiccdown-ex type phase reason message {}))
  ([type phase reason message data]
   (ex-info message (merge {:type type
                            :phase phase
                            :reason reason}
                           data))))

(defn parse!
  ([phase reason message]
   (throw (hiccdown-ex :hiccdown.error/parse phase reason message)))
  ([phase reason message data]
   (throw (hiccdown-ex :hiccdown.error/parse phase reason message data))))

(defn render!
  ([phase reason message]
   (throw (hiccdown-ex :hiccdown.error/render phase reason message)))
  ([phase reason message data]
   (throw (hiccdown-ex :hiccdown.error/render phase reason message data))))

(defn unsupported!
  ([phase reason message]
   (throw (hiccdown-ex :hiccdown.error/unsupported phase reason message)))
  ([phase reason message data]
   (throw (hiccdown-ex :hiccdown.error/unsupported phase reason message data))))

(defn invalid-ir!
  ([phase reason message]
   (throw (hiccdown-ex :hiccdown.error/invalid-ir phase reason message)))
  ([phase reason message data]
   (throw (hiccdown-ex :hiccdown.error/invalid-ir phase reason message data))))

(defn invalid-hiccup!
  ([phase reason message]
   (throw (hiccdown-ex :hiccdown.error/invalid-hiccup phase reason message)))
  ([phase reason message data]
   (throw (hiccdown-ex :hiccdown.error/invalid-hiccup phase reason message data))))
