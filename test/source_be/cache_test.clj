(ns source-be.cache-test
  (:require [clojure.test :refer :all]
            [abantu.cache :as cache]))

(deftest use-cache
  (testing "adding, retrieving and removing values in cache by uuid"
    (let [google-test-cache (cache/create-cache)
          emails-test-cache (cache/create-cache)
          google-result (do
                          (cache/add-item google-test-cache {:uri "http://yeet.com/test"} "testuuid")
                          (cache/get-item google-test-cache "testuuid"))
          emails-result (do
                          (cache/add-item emails-test-cache "test@toast.com" "testuuid")
                          (cache/get-item emails-test-cache "testuuid"))]

      (is (= (count (cache/get-all-items google-test-cache)) 1))
      (is (= (count (cache/get-all-items emails-test-cache)) 1))
      (is (= google-result {:uri "http://yeet.com/test"}))
      (is (= emails-result "test@toast.com"))

      (cache/add-item google-test-cache {:uri "http://delete.com/"} "todelete")
      (cache/add-item google-test-cache {:uri "http://dontdelete.com/"})
      (is (= (count (cache/get-all-items google-test-cache)) 3))

      (cache/add-item emails-test-cache "delete@delete.com" "todelete")
      (cache/add-item emails-test-cache "dontdelete@dontdelete.com")
      (is (= (count (cache/get-all-items emails-test-cache)) 3))

      (let [google-removed (do
                             (cache/remove-item google-test-cache "testuuid")
                             (cache/get-item google-test-cache "testuuid"))
            email-removed (do
                            (cache/remove-item emails-test-cache "testuuid")
                            (cache/get-item emails-test-cache "testuuid"))]

        (is (= (count (cache/get-all-items google-test-cache)) 2))
        (is (= (count (cache/get-all-items emails-test-cache)) 2))

        (is (not (some? google-removed)))
        (is (not (some? email-removed)))))))


(run-tests)
