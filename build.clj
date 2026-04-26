(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b]))

(def lib 'com.hjsoft/event-logger-backend)
(def version (format "0.1.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

;; delay to defer side effects (artifact downloads)
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn test [opts]
  (let [test-basis (b/create-basis {:project "deps.edn" :aliases [:test]})
        cmds (b/java-command
               {:basis test-basis
                :main "clojure.main"
                :main-args (into ["-m" "cognitect.test-runner" "-d" "src/test"]
                             (mapcat (fn [[k v]] [(str k) (str v)]) opts))})]
    (let [{:keys [exit]} (b/process cmds)]
      (when-not (zero? exit)
        (throw (ex-info "Tests failed" {:exit exit}))))))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src/main" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis @basis
                  :ns-compile '[event-logger-backend.core]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           :main 'event-logger-backend.core}))
