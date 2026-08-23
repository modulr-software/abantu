(ns abantu.services.comments.interface)

;; TODO: replace this private override with a proper map
(def ^:private ?User [:map [:id :int]])

(def ?Comment [:map
               [:exercise-id :int]
               [:unit-id :int]
               [:course-id :int]
               [:text :string]
               [:user ?User]
               [:timestamp :string]
               [:resolved :int]
               [:resolved-by [:or :int :nil]]
               [:resolved-at [:or :string :nil]]])
