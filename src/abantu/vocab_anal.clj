(ns abantu.vocab-anal
  (:require [wkok.openai-clojure.api :as openai]
            [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:import [java.util Base64]))

(def models {:mini "gpt-4.1-mini"
             :fivetwo "gpt-5.2"})

(defn model [key]
  (get-in models [key]))

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

(defn data-url
  "Takes an image-path on the local file system and converts the image to a base64 url.
   Returns a string."
  [image-path]
  (str "data:image/jpg;base64,"
       (image->base64 image-path)))

(defn image-payload [image-path]
  {:type "image_url"
   :image_url {:url (data-url image-path)}})

(defn prompt-text [prompt]
  {:type "text"
   :text prompt})

(defn prompt [model prompt & image-paths]
    {:model model
     :messages [{:role "user"
                 :content (-> (concat [(prompt-text prompt)]
                                  (mapv #(image-payload %)
                                        image-paths))
                              (vec))}]})

(defn extract-content [result]
  (-> result
      (get-in [:choices 0 :message :content])))

(defn gib-thing
  "takes a model name a text prompt and an optional set of image paths
   and returns the first result that gipitty produces, without the json payload around it."
  [prompt' model & image-paths]
  (-> prompt
      (partial model prompt')
      (apply image-paths)
      (openai/create-chat-completion)
      (extract-content)))


(defn prompt52 [text]
  (str "Extract all verb stems from the following text.
   Only list the verb stems and an english literal meaning of the verb stem.
   Produce only this and nothing else.
   Do not include pronoun prefixes on any of the verbs, instead start the verbs witht he character '-'.
   For example: walinyhala becomes -linyhala. ebathaza becomes -thaza. sesabalimi becomes -limi.
   The text is:\n"
       text))


(def ^:private image-text-prompt
  "Extract the text in the given image and return only the text, nothing else. attempt to match the formatting of
   the text with whitespace characters, but do not use any markdown of any sort.")

(gib-thing
 image-text-prompt
 (model :mini)
 "resources/undlali_3.jpg")

(-> image-text-prompt
    (gib-thing (model :mini)
               "resources/undlali_3.jpg")
    (prompt52)
    (gib-thing (model :fivetwo)))


(comment

  ;; PRIOS
  ;; - load the latest vocab json into the db
  ;; - load more vocab from this list: https://www.scribd.com/document/523205194/1000-Most-Common-Xhosa-words
  ;; - complete a model for language and parts of speech + units and exercises
  ;; - complete API for being able to:
  ;; ---- look at vocab, parts of speech, units etc.
  ;; ---- be able to update all of the above

  ;; NICE TO HAVES
  ;; - testing that raw text extraction of verb stems, nouns and adverbs works fine
  ;; ---- verb stems can be extracted if examples of how to extract are supplied (results inconsistent)
  ;; ---- nouns can be extracted just fine afaik
  ;; - training / contextualizing a custom model with dictionary vocab
  ;; - prompt pipeline for expressions


  (defn test-prompt [text]
    (str "Extract from the given text all verb stems related to any of the verbs in the list of verbs supplied.
          Do this by dropping any prefixes and infixes that come before the verb stem you find in the given text.
          Do not change the verbs in the text to their root form, keep the variant, but show only the variant without
          the prefixes attached to it.
          Extract only the words that are related to the supplied list of verbs and nothing else.
          List only the verb stem without pronoun prefixes or object concords.
          Do not include any markdown characters in the output.
          Provide a description of the meaning of the extracted verb form.

          For example: when given -baleka as a supplied verb, extract from the text 'ndibalekile' the
          form '-balekile - to have run'.


          The list of verbs supplied is: -lunga (to become fine), -nxiba (to wear something).
          The given text is:\n"
         text))

  (->> (gib-thing
        (test-prompt text-cache)
        (model :fivetwo))
       (spit "resources/output2.txt"))



  (def text-cache
    (gib-thing
     image-text-prompt
     (model :mini)
     "resources/undlali_3.jpg"))

  text-cache
  (spit "resources/text_cache.txt" text-cache)



  (def dict-response
    (gib-thing
     (model :fivetwo)
     (str "extract all of the given isiXhosa words into a list of json objects with the following fields:
      - type (the part of speech)
      - noun_class (the prefix of singular and plural of the noun only)
      - word (the actual word or expression in the target language)
      - english (the english translation of the word)
      
      use the content of the text provided to fill the values of the json object fields.
      produce the raw json text output only without any formatting.
      
      the text is: "
          (slurp "resources/dict.json"))))



  (prompt52 text-cache)


  ()
  )
