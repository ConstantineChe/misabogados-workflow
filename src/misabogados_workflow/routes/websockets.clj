(ns misabogados-workflow.routes.websockets
  (:require [compojure.core :refer [GET POST defroutes wrap-routes]]
            [ring.util.response :refer [response]]
            [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as t])
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

(defn broadcast []
  (prn 0))

(defonce channels (atom #{}))

(defn notify-clients! [request channel msg]
  (doseq [channel @channels]
    (prn channel)
    (async/send! (:chan channel) msg)))

(defn connect! [request channel]
  (let [session (:value (get (:cookies request) "JSESSIONID"))]
    (log/info "channel open for session " session)
    (swap! channels conj {:session session
                          :chan channel})))

(defn disconnect! [request channel {:keys [code reason]}]
  (let [session (:value (get (:cookies request) "JSESSIONID"))]
    (log/info "close code:" code "reason:" reason)
    (swap! channels #(remove #{{:chan channel :session session}} %))))

(defn websocket-callbacks [request]
  "WebSocket callback functions"
  {:on-open (partial connect! request)
   :on-close (partial disconnect! request)
   :on-message (partial notify-clients! request)})

(defn ws-handler [request]
  (async/as-channel request (websocket-callbacks request)))

(defn ws-msg [{:keys [params]}]
  (notify-clients! 1 (write {:message "Hi"}))
  (response {:status "ok"}))

(defroutes websocket-routes
  (GET "/ws" [] ws-handler)
  (POST "/msg" [] ws-msg))
