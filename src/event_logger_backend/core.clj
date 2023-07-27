(ns event-logger-backend.core
  (:gen-class)
  (:require [org.httpkit.server :as hks]
            [ring.middleware.defaults :refer :all]
            [buddy.auth.middleware :as buddy]
            [buddy.auth.backends :as backends]
            [reitit.ring :as ring]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [tick.core :as t]))

(def url-base "")

(def realm "event-logger")

(defonce storage (atom {}))

(defn url
  [u]
  (str url-base u))

(defn api-response
  [code response-map]
  {:status code
   :headers {"Content-Type" "application/json"}
   :body (json/write-str response-map)})

(defn not-found
  [& _]
  (api-response
   404
   {:error "Not Found"}))

(defn unauthorized
  [& _]
  {:status 401
   :headers {"Content-Type" "application/json"
             "WWW-Authenticate" (format "Basic realm=%s" realm)}
   :body (json/write-str {:error "Unauthorized"})})

(defn ping-handler
  [req]
  (api-response 200 {:response "pong"}))

(defn get-logger
  [req]
  (get-in req [:path-params :id]))

(defn download-handler
  [req]
  (if-not (:identity req)
    (not-found)
    (let [logger (get-logger req)]
      (api-response
        200
        {:categories (get-in
                       @storage
                       [(get-logger req) :categories])
         :date (get-in @storage [(get-logger req) :date])}))))

(defn save-logger!
  [id categories]
  (swap!
    storage
    (fn [s]
      (->
        s
        (assoc-in [id :date] (t/now))
        (assoc-in [id :categories] categories)))))

(defn upload-handler
  [req]
  (if-not (:identity req)
    (not-found)
    (let [request-body (json/read-str
                         (ring.util.request/body-string req)
                         :key-fn keyword)
          categories (:categories request-body)
          _ (prn request-body)
          id (get-logger req)]
      (save-logger! id categories)
      (api-response
        200
        {:response request-body}))))

(defn register-logger!
  [id login password]
  (swap!
   storage
   assoc
   id
   {:login login
    :password password
    :categories []
    :date (t/now)}))

(defn unregister-logger!
  [id]
  (swap!
    storage
    dissoc
    id))

(defn owner?
  [login logger]
  (= (:login logger) login))

(defn register-handler
  [req]
  (let [id (get-logger req)
        login (get-in req [:params :login])
        password (get-in req [:params :password])]
    (if (get @storage id)
      (api-response 200 {:response (format "'%s' already exists" id)})
      (do
        (register-logger! id login password)
        (api-response 200 {:response (format "'%s' created" id)})))))

(defn unregister-handler
  [req]
  (let [id (get-logger req)
        logger (get @storage id)
        login (:identity req)]
    (if-not (and logger (owner? login logger))
      (not-found)
      (do
        (unregister-logger! id)
        (api-response 200 {:response (format "'%s' deleted" id)}))
      )))

(defn my-authfn
  [req authdata]
  (let [login (:username authdata)
        password (:password authdata)
        id (get-logger req)
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
                        :post upload-handler
                        :delete unregister-handler}]]]
      (ring/router)
      (ring/ring-handler not-found)
      (wrap-defaults
       (assoc
        api-defaults
        :proxy true
        :static {:resources "public"}))))

(defonce server (atom nil))

(defn stop-server!
  []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server!
  []
  (reset! server (hks/run-server #'app {:port 8080})))

(defn -main [& args]
  (start-server!))

(comment
  (start-server!)

  (stop-server!)

  (app {:scheme :http :request-method :get :uri "/api/logger/x"})

  @storage

  (swap! storage dissoc "mine")


;
  )
