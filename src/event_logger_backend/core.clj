(ns event-logger-backend.core
  (:gen-class)
  (:require [org.httpkit.server :as hks]
            [ring.middleware.defaults :refer :all]
            [buddy.auth.middleware :as buddy]
            [buddy.auth.backends :as backends]
            [reitit.ring :as ring]
            [clojure.data.json :as json]))

(def url-base "")

(def realm "event-logger")

(defonce storage (atom {}))

(defn url [u]
  (str url-base u))

(defn not-found
  [& _]
  {:status 404
   :headers {"Content-Type" "text/plain"}
   :body "Not Found"})

(defn unauthorized
  [& _]
  {:status 401
   :headers {"Content-Type" "text/plain"
             "WWW-Authenticate" (format "Basic realm=%s" realm)}
   :body "Authentication Required"})

(defn ping-handler
  [req]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "pong"})

(defn get-id
  [req]
  (get-in req [:path-params :id]))

(defn download-handler
  [req]
  (if-not (:identity req)
    (unauthorized)
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str
            {:categories (get-in
                          @storage
                          [(get-id req) :categories])})}))

(defn upload-handler
  [req]
  (if-not (:identity req)
    (unauthorized)
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/write-str
            {:response req})}))

(defn register-logger!
  [id login password]
  (swap!
   storage
   assoc
   id
   {:login login
    :password password
    :categories []
    :date nil}))

(defn register-handler
  [req]
  (let [id (get-id req)
        login (get-in req [:params :login])
        password (get-in req [:params :password])]
    (if (get @storage id)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:response (format "'%s' already exists" id)})}
      (do
        (register-logger! id login password)
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:response (format "'%s' created" id)})}))))

(defn my-authfn
  [req authdata]
  (let [login (:username authdata)
        password (:password authdata)
        id (get-id req)
        existing-logger (get @storage id)]
    (when (= ((juxt :login :password) existing-logger) [login password])
      login)))

(def backend (backends/basic {:realm realm :authfn my-authfn}))

(defn authenticated-for-logger [handler]
  (buddy/wrap-authentication handler backend))

(def app
  (-> [["/api"
        ["/ping" ping-handler]
        ["/register/:id" {:post register-handler}]
        ["/logger/:id" {:middleware [authenticated-for-logger]
                        :get download-handler
                        :post upload-handler}]]]
      (ring/router)
      (ring/ring-handler not-found)
      (wrap-defaults
       (assoc
        api-defaults
        :proxy true
        :static {:resources "public"}))))

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

  (app {:scheme :http :request-method :get :uri "/api/logger/x"})

;
  )
