(ns event-logger-backend.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [event-logger-backend.core :refer [api-response not-found]]))

(deftest api-response-test
  (testing "api-response returns a map with correct status and body"
    (let [response (api-response 200 "OK")]
      (is (= 200 (:status response)))
      (is (= "OK" (:body response)))
      (is (= {"Content-Type" "text/plain"} (:headers response))))))

(deftest not-found-test
  (testing "not-found returns a 404 response"
    (let [response (not-found)]
      (is (= 404 (:status response)))
      (is (= "Not Found" (:body response))))))

(deftest ping-handler-test
  (testing "ping-handler returns a 200 pong response"
    (let [response (event-logger-backend.core/ping-handler {})]
      (is (= 200 (:status response)))
      (is (= "pong" (:body response))))))

(deftest register-handler-test
  (testing "register-handler creates a new logger"
    (reset! event-logger-backend.core/storage {})
    (let [req {:params {:id "test-id" :login "user" :password "pass"}}
          response (event-logger-backend.core/register-handler req)]
      (is (= 200 (:status response)))
      (is (re-find #"'test-id' created" (:body response)))
      (is (= {:login "user" :password "pass" :document nil}
             (get @event-logger-backend.core/storage "test-id")))))

  (testing "register-handler returns an error if logger already exists"
    (reset! event-logger-backend.core/storage {"test-id" {:login "user"}})
    (let [req {:params {:id "test-id" :login "new-user" :password "new-pass"}}
          response (event-logger-backend.core/register-handler req)]
      (is (= 200 (:status response)))
      (is (re-find #"'test-id' already exists" (:body response))))))

(deftest unregister-handler-test
  (testing "unregister-handler deletes a logger if authorized"
    (reset! event-logger-backend.core/storage {"test-id" {:login "user"}})
    (let [req {:path-params {:id "test-id"} :identity "user"}
          response (event-logger-backend.core/unregister-handler req)]
      (is (= 200 (:status response)))
      (is (= "'test-id' deleted" (:body response)))
      (is (nil? (get @event-logger-backend.core/storage "test-id")))))

  (testing "unregister-handler returns 404 if not authorized"
    (reset! event-logger-backend.core/storage {"test-id" {:login "user"}})
    (let [req {:path-params {:id "test-id"} :identity "wrong-user"}
          response (event-logger-backend.core/unregister-handler req)]
      (is (= 404 (:status response))))))

(deftest upload-handler-test
  (testing "upload-handler saves document if authorized"
    (reset! event-logger-backend.core/storage {"test-id" {:login "user"}})
    (let [req {:path-params {:id "test-id"}
               :identity "user"
               :body (java.io.ByteArrayInputStream. (.getBytes "test-content"))}
          response (event-logger-backend.core/upload-handler req)]
      (is (= 200 (:status response)))
      (is (= "test-content" (:body response)))
      (is (= "test-content" (get-in @event-logger-backend.core/storage ["test-id" :document])))))

  (testing "upload-handler returns 404 if not authorized"
    (let [req {:path-params {:id "test-id"} :identity nil}
          response (event-logger-backend.core/upload-handler req)]
      (is (= 404 (:status response))))))

(deftest download-handler-test
  (testing "download-handler returns document if authorized"
    (reset! event-logger-backend.core/storage {"test-id" {:login "user" :document "saved-content"}})
    (let [req {:path-params {:id "test-id"} :identity "user"}
          response (event-logger-backend.core/download-handler req)]
      (is (= 200 (:status response)))
      (is (= "saved-content" (:body response)))))

  (testing "download-handler returns 404 if not authorized"
    (let [req {:path-params {:id "test-id"} :identity nil}
          response (event-logger-backend.core/download-handler req)]
      (is (= 404 (:status response))))))

(deftest my-authfn-test
  (testing "my-authfn returns login if credentials match"
    (reset! event-logger-backend.core/storage {"test-id" {:login "user" :password "pass"}})
    (let [req {:path-params {:id "test-id"}}
          authdata {:username "user" :password "pass"}]
      (is (= "user" (event-logger-backend.core/my-authfn req authdata)))))

  (testing "my-authfn returns nil if credentials don't match"
    (reset! event-logger-backend.core/storage {"test-id" {:login "user" :password "pass"}})
    (let [req {:path-params {:id "test-id"}}
          authdata {:username "user" :password "wrong-pass"}]
      (is (nil? (event-logger-backend.core/my-authfn req authdata))))))
