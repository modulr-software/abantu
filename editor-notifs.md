# Scope: Notify users added to a course as editors

Send an email to a creator when they're granted edit rights on a course.
Trigger point: `add-editor` in `src/abantu/routes/api/courses.clj`.

## Existing machinery (reused)

- `async/handle-command :send-email` → fire-and-forget SMTP via `email/gmail.clj`.
- `email/templates.clj` → hiccup HTML template pattern (`exercise-comment-notification` is the model).
- Recipient is always a `creator` (enforced by a 403 above), so no admin-filter/list
  logic is needed — unlike the comment notifier, the data is already bound in the route,
  so the call is inlined rather than placed in a new `email/courses.clj`.
- `conf/read-cors-with-port` returns a **regex** (CORS `Origin` matcher), not a string — do not use it for email links. New `read-api-url`, `read-admin-portal-url`, and `read-app-url` (step 1) provide the string origins per env.

## Changes (3 files)

### 1. [x] `resources/config.edn` + `.env` + `src/abantu/config.clj` — env-aware URL config

Add three config fields — `:base-url` (the API/backend), `:admin-portal-url`
(the dashboard), and `:app-url` (the learner app) — each overridable via env
(`BASE_URL`, `ADMIN_PORTAL_URL`, `APP_URL`) with prod defaults. Three
independent readers, no shared helper:

- `read-api-url` — env-conditional: prod reads `:base-url` (a domain, no
  port); non-prod builds `http://localhost:<port>` from `:port`. `:port` is
  in-repo config for the backend's own port only.
- `read-admin-portal-url` — straight `(read-value :admin-portal-url)`. No
  port building; set `ADMIN_PORTAL_URL` in `.env` to point elsewhere.
- `read-app-url` — straight `(read-value :app-url)`. Same as above.

`read-cors-with-port` stays a regex (CORS matcher); these are its string
counterparts. This feature uses `read-admin-portal-url` only (the
editor-added email's button → dashboard); the other two are infrastructure
for future emails.

**`resources/config.edn`** — flip the `:env` default from `"dev"` to `"prod"`
and add the three URL fields:

```clojure
 :env #or [#env ENV "prod"]
 :base-url #or [#env BASE_URL "https://abantu-api.modulrza.app"]
 :admin-portal-url #or [#env ADMIN_PORTAL_URL "https://abantu-portal.modulrza.app"]
 :app-url #or [#env APP_URL "https://abantu.modulrza.app"]
```

**`.env`** — add `ENV=dev` so the local repo overrides the prod default. In
dev `read-api-url` auto-builds `http://localhost:3001`; the other two fall
back to their prod defaults unless overridden here. (`PORT=3001` is already
set.)

```
ENV=dev
```

**`src/abantu/config.clj`** — add the three fields to the schema:

```clojure
[:base-url :string]
[:admin-portal-url :string]
[:app-url :string]
```

and add the three readers (plus `(read-api-url)`, `(read-admin-portal-url)`,
`(read-app-url)` demo calls in the `(comment ...)` block, matching the
existing `(read-cors-with-port)` pattern):

```clojure
(defn read-api-url
  "API origin for the current env. Prod reads :base-url; non-prod builds
  http://localhost:<port> from :port. `read-cors-with-port` is a CORS regex
  matcher; this is its string counterpart."
  []
  (case (read-value :env)
    "prod" (read-value :base-url)
    (str "http://localhost:" (read-value :port))))

(defn read-admin-portal-url
  "Admin portal origin (dashboard) for email links. Read straight from config;
  set ADMIN_PORTAL_URL in .env to override."
  []
  (read-value :admin-portal-url))

(defn read-app-url
  "App origin (learner app) for email links. Read straight from config;
  set APP_URL in .env to override."
  []
  (read-value :app-url))
```

### 2. [x] `src/abantu/email/templates.clj`

Add `editor-added-notification`, mirroring `exercise-comment-notification`.
Inputs: `{:recipient-name :course-name :course-id}`. Button → `/dashboard/courses/<id>`.

```clojure
(defn editor-added-notification
  "Returns the completed HTML for an editor-added notification email.
  Sent to a creator who has just been granted edit rights on a course."
  [{:keys [recipient-name course-name course-id]}]
  (h/html5
   {:lang "en"}
   (head-metadata)
   [:body {:style "font-family: 'Switzer', sans-serif"}
    [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
     [:tr
      [:td {:align "center" :style "padding: 20px;"}
       [:table {:class "content"
                :width "600"
                :border "0"
                :cellspacing "0"
                :cellpadding "0"
                :style "border-collapse: collapse; border: 1px solid #cccccc;"}
        (header)
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          (str "Hi " recipient-name ",")
          [:br] [:br]
          (str "You've been added as an editor of the course \"" course-name "\".")
          [:br] [:br]
          "You can now edit its units and exercises."]]
        (button {:text "View Course"
                 :redirect (str (conf/read-admin-portal-url) "/dashboard/courses/" course-id)})
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          "Regards,"
          [:br]
          "The Abantu Team"]]
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 11px; line-height: 1.6;"}
          "This is an automated message. Please do not reply directly to this email."]]
        (footer)]]]]]))
```

### 3. [x] `src/abantu/routes/api/courses.clj`

Add requires:

```clojure
            [abantu.async.interface :as async]
            [abantu.email.templates :as templates]
```

Rewrite the `:else` branch of `add-editor` so the email fires **only when
`add-editor!` actually inserts** (it returns `nil` if the editor already
existed). This also makes the response honest about no-ops — currently it
always says "Successfully added" even on a re-add.

```clojure
      :else
      (if (courses/add-editor! ds target-id course-id)
        (do (async/handle-command
             :send-email
             {:to (:email target)
              :subject (str "You've been added as an editor of \"" (:name course) "\"")
              :type :text/html
              :body (templates/editor-added-notification
                     {:recipient-name (:firstname target)
                      :course-name (:name course)
                      :course-id course-id})})
            (res/response {:message (str "Successfully added editor '" target-id "' to course '" course-id "'.")}))
        (res/response {:message (str "User '" target-id "' is already an editor of this course.")})))))
```
