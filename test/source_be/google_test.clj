(ns source-be.google-test
  (:require [clojure.test :refer :all]
            [abantu.cache :as cache]
            [abantu.oauth2.google.core :as google]
            [clj-oauth2.client :as oauth2]))

(defn oauth2-get [uri headers]
  (let [auth-header (get-in
                     headers
                     [:headers "Authorization"])]
    (if (and (some? uri) (= "Bearer access-token" auth-header))
      {:body "{\"email\":\"toast@toast.com\"}"}
      {:body nil})))

(defn oauth2-get-access-token
  [endpoint
   & [params {expected-state :state expected-scope :scope}]]
  (when (some? (:code params))
    {:access-token "access-token"}))

(defn google-auth-flow-test-case
  [mock-code
   expected-email
   get-access-token-override
   get-override]

  (with-redefs [oauth2/get-access-token get-access-token-override
                oauth2/get get-override]

    (let [auth-reqs-service (cache/create-cache)
          auth-req (google/-auth-uri auth-reqs-service)]

      (is (some? (:uri auth-req)))
      (is (some? (:uuid auth-req)))
      (is (= (count (cache/get-all-items auth-reqs-service)) 1))

      (let [email (google/-google-session-user
                   auth-reqs-service
                   (:uuid auth-req)
                   mock-code)]

        (is (= expected-email email))))))

(deftest google-auth-flow-test
  (testing "testing the google oauth2 flow"

        (testing "successful auth flow"
          (google-auth-flow-test-case 
            {:code "a1b2c3"}
            "toast@toast.com"
            oauth2-get-access-token
            oauth2-get))
        
        (testing "missing code verifier"
          (google-auth-flow-test-case 
            {}
            nil
            oauth2-get-access-token
            oauth2-get))))
