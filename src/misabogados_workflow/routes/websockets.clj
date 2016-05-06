(ns misabogados-workflow.routes.websockets
  (:require [compojure.core :refer [GET POST defroutes wrap-routes]]
            [ring.util.response :refer [response]]
            [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as t]
            [clojure.core.async :as a :refer [go-loop <!]])
  (import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn- current-time []
  (quot (System/currentTimeMillis) 1000))

(defn write [x]
  (let [baos (ByteArrayOutputStream.)
        w    (t/writer baos :json)
        _    (t/write w x)
        ret  (.toString baos)]
    (.reset baos)
    ret))



(defn read [msg]
  (let [bytes (.getBytes msg)
        bios  (ByteArrayInputStream. bytes)
        r     (t/reader bios :json)
        ret (t/read r)]
    (.reset bios)
    ret))

(read (write [1 2 3]))

(defonce sessions
  (atom {}))

(defonce channels (atom #{}))

(defn process-message [request channel msg]
  (let [session (:value (get (:cookies request) "JSESSIONID"))]
    (case (:code message)
      :extend (swap! sessions assoc-in [(keyword session) :timeout]
                     (+ 30 (current-time)))
      :default
      (doseq [channel @channels]
        (prn channel)
        (async/send! (:chan channel) msg)))))

(defn connect! [request channel]
  (let [session (:value (get (:cookies request) "JSESSIONID"))]
    (log/info "channel open for session " session)
    (swap! channels conj {:session session
                          :chan channel})
    (when-not ((keyword session) @sessions)
        (swap! sessions assoc (keyword session) {:timeout (+ 30 (current-time))}))))

(defn disconnect! [request channel {:keys [code reason]}]
  (let [session (:value (get (:cookies request) "JSESSIONID"))]
    (log/info "close code:" code "reason:" reason)
    (swap! channels #(remove #{{:chan channel :session session}} %))))

(defn websocket-callbacks [request]
  "WebSocket callback functions"
  {:on-open (partial connect! request)
   :on-close (partial disconnect! request)
   :on-message (partial process-message request)})

(defn ws-handler [request]
  (async/as-channel request (websocket-callbacks request)))

(defn ws-msg [request]
  (let [params (:params request)]
    (notify-clients! request 1 (write {:message "Hi"}))
    (response {:status "ok"})))

(defn check-sessions []
  (doseq [channel @channels]
    (let [session ((keyword (:session channel)) @sessions)
          timeout (:timeout session)]
      (if (> (current-time) timeout)
        (async/send! (:chan channel) (write {:message "session timed out"}))))))

(defn start-sessions-checker! []
  (go-loop [i 0]
    (<! (a/timeout 10000))
    (notify-clients! nil 1 (write {:message i}))
    (check-sessions)
    (recur (inc i))))

(start-sessions-checker!)

(defroutes websocket-routes
  (GET "/ws" [] ws-handler)
  (POST "/msg" [] ws-msg))
