(ns misabogados-workflow.util
  (:require [clojure.string :as str]
            [clj-recaptcha.client-v2 :as c]
            [clojure.walk :as walk]
            [clj-time.local :as l]))

(defn remove-kebab [str]
  "removes kebab and makes string human-eatable"
  (if str (str/capitalize (str/replace str "-" " "))))

(defn check-recaptcha [params]
  ;; (c/verify "6Lc92P4SAAAAAMydKZy-wL7PAUTJghmVU7sXfehY" (:g-recaptcha-response params))
  (c/verify "6Lco-wsTAAAAAILKmwTLCAXu2WWwSXHPs3VyK1l5" (:g-recaptcha-response params)))

(defn generate-hash [something]
  (let [hash-bytes
        (doto (java.security.MessageDigest/getInstance "MD5")
          (.reset)
          (.update (.getBytes (str something (new java.util.Date)))))]
    (.toString
     (new java.math.BigInteger 1 (.digest hash-bytes))
     32)))

(defn md5 [s]
  (let [algorithm (java.security.MessageDigest/getInstance "MD5")
        size (* 2 (.getDigestLength algorithm))
        raw (.digest algorithm (.getBytes s))
        sig (.toString (java.math.BigInteger. 1 raw) 16)
        padding (apply str (repeat (- size (count sig)) "0"))]
    (str padding sig)))

(defn base-path [request]
  (str (name (:scheme request)) "://" (:server-name request)))

(defn full-path [request rel-path]
  (str (base-path request) rel-path))

(defn wrap-datetime [params]
  (walk/postwalk
   #(if (and (string? %)
             (re-matches #"^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}).+" %))
      (try (l/to-local-date-time %)
           (catch Exception e  %)) %)
   params))
