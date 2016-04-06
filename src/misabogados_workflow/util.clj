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

(defn md5 [s]
  (let [algorithm (java.security.MessageDigest/getInstance "MD5")
        size (* 2 (.getDigestLength algorithm))
        raw (.digest algorithm (.getBytes s))
        sig (.toString (java.math.BigInteger. 1 raw) 16)
        padding (apply str (repeat (- size (count sig)) "0"))]
    (str padding sig)))

(defn base-path [request]
  (str (name (:scheme request)) "://" (:server-name request) ":" (:server-port request)))

(defn full-path [request rel-path]
  (str (base-path request) rel-path))
