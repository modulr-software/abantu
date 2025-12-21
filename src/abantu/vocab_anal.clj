(ns abantu.vocab-anal
  (:require [wkok.openai-clojure.api :as openai]
            [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:import [java.util Base64]))

(defn image->base64 [path]
  (let [file-bytes (with-open [in (io/input-stream (io/file path))]
                     (.readAllBytes in)) ; Read all bytes from the input stream
        encoder (Base64/getEncoder)
        encoded-bytes (.encode encoder file-bytes)]
    (String. encoded-bytes)))

(def noun-extraction-query
  "The picture provided contains isiXhosa text. Please return the following information for all the nouns in this text in JSON format as an array of objects, avoiding duplicates as much as possible: 

xhosa: The normalised, base form of the xhosa word including its noun class without any other infixes or postfixes. This must be a noun that has a class, it cannot be another part of speech used as a noun, for example, gerunds may not be included. Proper nouns may be included. Please include the base class prefixes next to the noun separated by a slash, like  \"umntu um-/aba-\". Make sure to change the prefix back to its original class, don't use just the form that you read.

english: The meaning of the noun in english. If there are multiple possible meanings, they can be separated by slashes. 

noun_class: The noun class numbers in both singular and plural forms, returned as a string with the class numbers separated by slashes. Return only the singular if there is no plural. For example, 5/6. 

type: noun. 

Please respond only with the desired output, in raw text, without any markdown formatting or newlines. Specifically ensure the output does not contain ```json ```, as this output is being returned directly from an API.")

(def verb-extraction-query
  "The picture provided contains isiXhosa text. Please return the following information for all the verbs in this text in JSON format as an array of objects, avoiding duplicates as much as possible: 
  
  xhosa: The root of verb, without any prefixes, infixes or postfixes attached. This field should start with -. There cannot be any duplicates of this field. 
  
  english: The meaning of the verb in english. If there are multiple possible meanings, they can be separated by slashes. 
  
  type: verb. 

  Please respond only with the desired output, in raw text, without any markdown formatting or newlines. Specifically ensure the output does not contain ```json ```, as this output is being returned directly from an API.")

(defn GIVE-ME-SOMETHING-BY-PROMPT-PLEEEAAASSEEEE [prompt image-path]
  (-> (openai/create-chat-completion {:model "gpt-4.1-mini"
                                      :messages [{:role "user"
                                                  :content [{:type "text"
                                                             :text prompt}
                                                            {:type "image_url"
                                                             :image_url {:url (str "data:image/jpg;base64," (image->base64 image-path))}}]}]})
      (get :choices)
      (first)
      (get-in [:message :content])
      (json/read-str {:key-fn keyword}))
  )

(comment

  (GIVE-ME-SOMETHING-BY-PROMPT-PLEEEAAASSEEEE noun-extraction-query "resources/undlali_3.jpg")

  ())
