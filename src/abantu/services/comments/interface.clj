(ns abantu.services.comments.interface)

(def ?Comment [:map
               [:exercise-id :int]
               [:unit-id :int]
               [:course-id :int]
               [:text :string]
               [:user-id [:or :int :nil]]
               [:timestamp :string]
               [:resolved :int]
               [:resolved-by [:or :int :nil]]
               [:resolved-at [:or :string :nil]]])
