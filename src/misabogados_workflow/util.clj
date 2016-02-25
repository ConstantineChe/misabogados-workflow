(ns misabogados-workflow.util
  (:require [clojure.string :as str]))

(defn remove-kebab [str]
  "removes kebab and makes string human-eatable"
  (if str (str/capitalize (str/replace str "-" " "))))

(defn generate-hash [something]
  (let [hash-bytes
        (doto (java.security.MessageDigest/getInstance "SHA-256")
          (.reset)
          (.update (.getBytes (str something (new java.util.Date)))))]
    (.toString
     (new java.math.BigInteger 1 (.digest hash-bytes))
     16)))
