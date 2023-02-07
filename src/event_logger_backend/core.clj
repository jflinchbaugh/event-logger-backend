(ns event-logger-backend.core
  (:gen-class)
  (:require [org.httpkit.server :as hks]
            [reitit.ring :as ring]))

(defn handler [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (str "hello HTTP! " req)})

(def app
  (ring/ring-handler
    (ring/router
      [["/api" ["/upload" {:get handler}]]
       ])))

(defonce server (atom nil))

(defn stop-server! []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server! []
  ;; The #' is useful when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and https://http-kit.github.io/migration.html#reload
  (reset! server (hks/run-server #'app {:port 8080})))

(defn -main [& args]
  (start-server!))

(comment
  (start-server!)

  (stop-server!)

  (app {:request-method :get :uri "/thing"})
  (app {:request-method :get :uri "/api/upload"})

;
  )
