# Scope: Course editors

A new `course-editors` join table in the master DB tracks which creators are
allowed to edit courses they don't own. Admins implicitly edit everything
(never in this table); students never edit. Only the course's creator (or an
admin) can add/remove editors.

## 1. ✅ Table — `src/abantu/db/master.clj`

Add `course-editors`, mirroring `user-courses`:

```clj
(def course-editors
  (tables/create-table-sql
   :course-editors
   (tables/table-id)
   [:user-id :int :not nil]
   [:course-id :int :not nil]
   (tables/foreign-key :user-id :users :id)
   (tables/foreign-key :course-id :courses :id)))
```

## 2. ✅ Migration — `src/abantu/migrations/003_course_editors.clj`

```clj
(ns abantu.migrations.003-course-editors
  (:require [abantu.db.master]
            [abantu.db.tables :as tables]))

(defn run-up! [context]
  (let [ds-master (:db-master context)]
    (tables/create-tables!
     ds-master
     :abantu.db.master
     [:course-editors])))

(defn run-down! [context]
  (let [ds-master (:db-master context)]
    (tables/drop-tables!
     ds-master
     [:course-editors])))
```

## 3. ✅ Service layer — `src/abantu/services/courses.clj`

```clj
(defn add-editor!
  "Grants a user edit rights on a course. Idempotent: no-op if already an editor."
  [ds user-id course-id]
  (let [exists? (db/exists? ds {:tname :course-editors
                                :where [:and
                                        [:= :user-id user-id]
                                        [:= :course-id course-id]]})]
    (when (not exists?)
      (db/insert! ds {:tname :course-editors
                      :data {:user-id user-id
                             :course-id course-id}
                      :ret :1}))))

(defn remove-editor!
  "Revokes a user's edit rights on a course."
  [ds user-id course-id]
  (db/delete! ds {:tname :course-editors
                  :where [:and
                          [:= :user-id user-id]
                          [:= :course-id course-id]]
                  :ret :1}))

(defn editors-by-course
  "Returns the public user records of every editor of the given course."
  [ds course-id]
  (let [user-ids (mapv :user-id (db/find ds {:tname :course-editors
                                             :where [:= :course-id course-id]
                                             :ret :*}))]
    (mapv (partial users/user ds) user-ids)))

(defn courses-by-editor
  "Returns the courses a user is allowed to edit (via the course-editors table)."
  [ds user-id]
  (let [course-ids (mapv :course-id (db/find ds {:tname :course-editors
                                                 :where [:= :user-id user-id]
                                                 :ret :*}))
        courses (db/find ds {:tname :courses
                             :where [:in :id course-ids]
                             :ret :*})]
    (mapv (comp process-bools
                (partial append-units ds)
                (partial append-creator ds)) courses)))

(defn editor?
  "True if user-id has edit rights on course-id via the course-editors table."
  [ds user-id course-id]
  (some? (db/find-one ds {:tname :course-editors
                          :where [:and
                                  [:= :user-id user-id]
                                  [:= :course-id course-id]]})))
```

## 4. ✅ OpenAPI schema — `src/abantu/routes/openapi.clj`

```clj
(def EditorTargetParams [:map [:user-id :int]])
```

(`api/User` is reused for the `list` response shape.)

## 5. ✅ Routes — `src/abantu/routes/api/courses.clj`

```clj
(defn list-editors
  {:summary "Get all editors for a given course. Only the course creator or an admin may call this."
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success [:vector api/User])
                  (api/not-found)
                  (api/response 403 (api/error)))}
  [{:keys [ds path-params] :as _request}]
  (let [course-id (parse-long (str (:id path-params)))]
    (if (courses/get-course ds course-id)
      (res/response (courses/editors-by-course ds course-id))
      (-> (res/response {:message (str "The course with the id '" course-id "' does not exist.")})
          (res/status 404)))))

(defn add-editor
  {:summary "Grant a creator edit rights on a course. Only the course creator or an admin may call this."
   :parameters (api/params :path api/IdPathParam :body api/EditorTargetParams)
   :responses (-> (api/success [:map [:message :string]])
                  (api/not-found)
                  (api/response 403 (api/error)))}
  [{:keys [ds path-params body course] :as _request}]
  (let [course-id (parse-long (str (:id path-params)))
        target-id (:user-id body)
        target (users/get-user ds target-id)]
    (cond
      (nil? target)
      (-> (res/response {:message (str "The user with the id '" target-id "' does not exist.")})
          (res/status 404))

      (not= "creator" (:role target))
      (-> (res/response {:message "Only creators can be added as editors."})
          (res/status 403))

      (= (:id target) (get-in course [:creator :id]))
      (-> (res/response {:message "The course creator is already an editor of this course."})
          (res/status 403))

      :else
      (do (courses/add-editor! ds target-id course-id)
          (res/response {:message (str "Successfully added editor '" target-id "' to course '" course-id "'.")})))))

(defn remove-editor
  {:summary "Revoke a creator's edit rights on a course. Only the course creator or an admin may call this."
   :parameters (api/params :path api/IdPathParam :body api/EditorTargetParams)
   :responses (-> (api/success [:map [:message :string]])
                  (api/not-found)
                  (api/response 403 (api/error)))}
  [{:keys [ds path-params body course] :as _request}]
  (let [course-id (parse-long (str (:id path-params)))
        target-id (:user-id body)
        target (users/get-user ds target-id)]
    (cond
      (nil? target)
      (-> (res/response {:message (str "The user with the id '" target-id "' does not exist.")})
          (res/status 404))

      (not= "creator" (:role target))
      (-> (res/response {:message "Only creators can be editors of a course."})
          (res/status 403))

      (not (courses/editor? ds target-id course-id))
      (-> (res/response {:message (str "User '" target-id "' is not an editor of this course.")})
          (res/status 404))

      :else
      (do (courses/remove-editor! ds target-id course-id)
          (res/response {:message (str "Successfully removed editor '" target-id "' from course '" course-id "'.")})))))
```

## 6. ✅ Route wiring — `src/abantu/routes/reitit.clj`

```clj
["/courses/:id/editors/list" (-> (get courses/list-editors)
                                 (mw authmw/wrap-owner-or-admin)
                                 (tag :courses))]

["/courses/:id/editors/add" (-> (post courses/add-editor)
                                (mw authmw/wrap-owner-or-admin)
                                (tag :courses))]

["/courses/:id/editors/remove" (-> (post courses/remove-editor)
                                   (mw authmw/wrap-owner-or-admin)
                                   (tag :courses))]
```

## Validation summary

- `list`: `wrap-owner-or-admin` → 401 unauthenticated, 404 course missing,
  403 non-owner creator; in-handler 404 on missing course (defensive).
  Returns the editor list.
- `add`: `wrap-owner-or-admin` → requester gated as above and `:course`
  assocs; in-handler 404 if target user missing, 403 if target isn't a
  creator or is the course's own creator, else success.
- `remove`: same requester gating; in-handler 404 if target user missing
  or not currently an editor, 403 if target isn't a creator, else success.

## Out of scope

- Integrating `courses-by-editor` into `get-all-courses` (admin-portal
  visibility for editors) — separate scope.
- Middleware that gates *other* course-mutation endpoints on `editor?` —
  not requested.

## Resolved

- Authorization (only course creator / admin can mutate the editor list) is
  enforced by `wrap-owner-or-admin` on all three routes — no duplicate
  in-handler check needed.
- `list` uses `wrap-owner-or-admin` (not `wrap-above-student`) so only the
  owner/admin sees the editor list.
