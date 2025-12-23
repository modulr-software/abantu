(ns abantu.routes.reitit
  (:require [reitit.ring :as ring]
            [abantu.middleware.interface :as mw]
            [abantu.db.interface :as db]
            [abantu.routes.file :as file]
            [abantu.routes.api.vocab.vocabs :as vocabs]
            [abantu.routes.api.vocab.-id-.vocab :as vocab]
            [abantu.routes.api.units.units :as units]
            [abantu.routes.api.units.-id-.unit :as unit]
            [abantu.routes.api.units.-id-.exercises.manage.exercises :as exercises]
            [abantu.routes.api.units.-id-.exercises.manage.-id-.exercise :as exercise]
            [abantu.routes.api.units.-id-.exercises.generate.exercises-generate :as exercises-generate]))

(defn create-app
  ([] (create-app {:ds (db/ds :master)}))
  ([{:keys [ds]}]
   (let [ds (or ds (db/ds :master))]
     (ring/ring-handler
      (ring/router
       [["/"              {:middleware [[mw/apply-generic :ds ds]]}
         [""              (fn [_request] {:status 200 :body {:message "success"}})]
         ["file"          {:post {:handler file/post}}]

         ["api"
          ["/vocab"
           [""            {:get {:handler vocabs/get}
                           :post {:handler vocabs/post}}]
           ["/:id"        {:get {:handler vocab/get}
                           :post {:handler vocab/post}
                           :delete {:handler vocab/delete}}]]

          ["/units"
           [""            {:get {:handler units/get}
                           :post {:handler units/post}}]
           ["/:id"
            [""           {:get {:handler unit/get}
                           :post {:handler unit/post}
                           :delete {:handler unit/delete}}]
            ["/exercises"
             ["/manage"
              [""         {:get {:handler exercises/get}
                           :post {:handler exercises/post}}]
              ["/:id"     {:get {:handler exercise/get}
                           :post {:handler exercise/post}
                           :delete {:handler exercise/delete}}]]
             ["/generate" {:get {:handler exercises-generate/get}}]]]]]]])))))

(comment
  ())
