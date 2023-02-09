(ns event-logger-backend.core
  (:gen-class)
  (:require [org.httpkit.server :as hks]
            [reitit.ring :as ring]))

(defn handler [req]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (str "hello HTTP! " req)})

(defn login-form [req]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (str "hello HTTP! " req)})

(defn login-action [req]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (str "hello HTTP! " req)})

(defn not-found [& _]
  {:status 404
   :headers {"Content-Type" "text/plain"}
   :body "Not Found"})

(def app
  (ring/ring-handler
    (ring/router
      [["/api"
        ["/upload" {:get handler}]
        ["/auth" {:get login-form
                  :post login-action}]]
       ])
    not-found))

(defonce server (atom nil))

(defn stop-server! []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server! []
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
