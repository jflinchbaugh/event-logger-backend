(ns event-logger-backend.core
  (:gen-class)
  (:require [org.httpkit.server :as hks]
            [reitit.ring :as ring]
            [ring.middleware.defaults :as rmd]
            [ring.util.request :as rur]
            [buddy.auth.middleware :as buddy]
            [buddy.auth.backends :as backends]
            [xtdb.client :as xtc]
            [xtdb.api :as xt]
            [xtdb.node :as xtn]
            [taoensso.timbre :as log]))

;;(log/merge-config! {:ns-filter #{"event-logger-backend.*"}})

(def ^:const realm "event-logger")

(def ^:const base-url "/storage")

(defonce storage (atom {}))

(defn api-response
  [code document]
  {:status code
   :headers {"Content-Type" "text/plain"}
   :body document})

(defn not-found
  [& _]
  (api-response
   404
   "Not Found"))

(defn unauthorized
  [& _]
  {:status 401
   :headers {"Content-Type" "text/plain"
             "WWW-Authenticate" (format "Basic realm=%s" realm)}
   :body "Unauthorized"})

(defn ping-handler
  [_]
  (api-response 200 "pong"))

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
        (get-in @storage [logger :document])))))

(defn save-logger!
  [id document]
  (swap!
    storage
    (fn [s]
      (->
        s
        (assoc-in [id :document] document))))
  (log/info (format "Saved document for '%s'" id)))

(defn upload-handler
  [req]
  (if-not (:identity req)
    (not-found)
    (let [request-body (rur/body-string req)
          id (get-logger req)]
      (save-logger! id request-body)
      (api-response
        200
        request-body))))

(defn register-logger!
  [id login password]
  (swap!
   storage
   assoc
   id
   {:login login
    :password password
    :document nil})
  (log/info (format "Registered '%s' for '%s'" id login)))

(defn unregister-logger!
  [id]
  (swap!
    storage
    dissoc
    id)
  (log/info (format "Unregistered '%s'" id)))

(defn owner?
  [login logger]
  (= (:login logger) login))

(defn register-handler
  [req]
  (let [id (get-in req [:params :id])
        login (get-in req [:params :login])
        password (get-in req [:params :password])]
    (if (get @storage id)
      (api-response 200 (format "'%s' already exists" id))
      (do
        (register-logger! id login password)
        (api-response 200 (format "'%s' created" id))))))

(defn unregister-handler
  [req]
  (let [id (get-logger req)
        logger (get @storage id)
        login (:identity req)]
    (if-not (and logger (owner? login logger))
      (not-found)
      (do
        (unregister-logger! id)
        (api-response 200 (format "'%s' deleted" id)))
      )))

(defn identity-required-wrapper [handler]
  (fn [req]
    (if (nil? (:identity req))
      (unauthorized)
      (handler req)))
  )

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
  (-> [base-url
       ["/api"
        ["/ping" ping-handler]
        ["/register" {:post register-handler}]
        ["/logger/:id" {:middleware
                        [authenticated-for-logger identity-required-wrapper]
                        :get download-handler
                        :post upload-handler
                        :delete unregister-handler}]]]
      (ring/router)
      (ring/ring-handler
        (ring/routes
          (ring/create-resource-handler {:path base-url})
          not-found))
      (rmd/wrap-defaults
       (assoc
        rmd/api-defaults
        :proxy true
        ))))

(defn connect-db
  "wire storage atom into xtdb"
  [xtdb-url]
  (remove-watch storage :to-xtdb)

  (with-open [node (xtc/start-client xtdb-url)]
    (->>
      (xt/q node '(from :loggers [_id document login password]))
      (reduce
        (fn [store doc] (assoc store (:xt/id doc) (dissoc doc :xt/id)))
        {})
      (reset! storage)))

  (add-watch storage :to-xtdb
    (fn [name atom old-val new-val]
      (let [old-keys (keys old-val)
            new-keys (keys new-val)
            removed (remove (set new-keys) old-keys)]
        (with-open [node (xtc/start-client xtdb-url)]
          (xt/submit-tx node
            (concat
              (for [id removed]
                [:delete-docs :loggers id])
              (for [doc new-val]
                [:put-docs :loggers
                (merge {:xt/id (first doc)} (second doc))]))))))))

(defonce server (atom nil))

(defn stop-server!
  []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server!
  [port xtdb-url]
  (if (nil? @server)
    (do
      (connect-db xtdb-url)
      (reset! server (hks/run-server #'app {:port port}))
      (log/info "Server started."))
    "server already running"))

(defn -main [& [port xtdb-url]]
  (if (or (nil? port) (nil? xtdb-url))
    (println "Usage: <port> <xtdb-url>")
      (start-server! (Integer/parseInt port) xtdb-url)
      ))

(comment

  (start-server! 8000 "http://localhost:3000")

  (stop-server!)

  (app {:scheme :http :request-method :get :uri "/api/logger/x"})

  (app {:scheme :http :request-method :get :uri "/api/logger/events"})

  (app {:scheme :http :request-method :get :uri "/api/ping"})

  @storage

  )
