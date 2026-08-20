(ns abantu.services.comments.interface)

(def ?Comment [:map
               [:exercise-id :int]
               [:unit-id :int]
               [:course-id :int]
               [:text :string]
               [:user-id :int]
               [:timestamp :string]
               [:resolved :int]
               [:resolved-by :int]
               [:resolved-at :string]])
