(ns reverie.batteries.objects.reset-password
  (:require [clojure.string :as str]
            [ez-form.core :refer [defform] :as form]
            [hiccup.core :refer [html]]
            [reverie.auth :as auth]
            [reverie.batteries.ez-form.wrappers]
            [reverie.core :refer [defobject defrenderer]]
            [reverie.email :as email]
            [reverie.i18n :refer [t]]
            [reverie.response :refer [redirect!]]
            [reverie.settings :as settings]
            [reverie.util :as util]
            [vlad.core :as vlad]))

(def send-email! nil)
(defmulti send-email! :status)
(defmethod send-email! :default [{:keys [to email_subject email_body url token]}]
  (let [body-text (format
                   "%s

%s
%s?token=%s"
                   email_body
                   (t :reverie.batteries.objects.reset-password/reset-link-text)
                   url
                   token)
        body-html [:div.wrapper
                   [:table {:border "0"}
                    [:tr [:td email_body]]
                    [:tr [:td]]
                    [:tr [:td [:a {:href (str url "?token=" token)}
                               (t :reverie.batteries.objects.reset-password/reset-link-text)]]]]]
        css "<style type=\"text/css\">
  .wrapper { width: 600px; }
  table {width: 100%; background-color: transparent;}
  td {border:0;}
</style>
"]
    (email/queue-message to email_subject
                         [:alternative
                          {:type "text/plain"
                           :content body-text}
                          {:type "text/html"
                           :content (html (list css
                                                body-html))}])))
(defn do-send-email! [{uri :uri scheme :scheme headers :headers {db :database settings :settings} :reverie :as request} {:keys [reset_url] :as properties} user]
  (let [url (if-not (str/blank? reset_url)
              reset_url
              (format "%s://%s%s" (name scheme) (get headers "host") uri))
        minutes (or (settings/get settings [:batteries :reset-password :minutes])
                    60)
        token (auth/enable-token (:id user) minutes db)]
    (send-email! (assoc properties
                        ;; define a method that caches ::catch
                        ;; for overriding the email functionality
                        :status ::catch
                        :to (:email user)
                        :url url
                        :token token))))

(defn delay-t [k] (fn ([_] (t k))
                     ([_ _] (t k))))

;;((delay-t :foo-bar) nil nil)

(def ^:dynamic *email-chart*
  [:table.table.reset-password.email
   [:tr :?email.wrapper
    [:th :$email.label]
    [:td
     :$email.field
     :$email.help
     :$email.errors]]
   [:tr
    [:td [:button.btn.btn-primary :?i18n.t :reverie.batteries.objects.reset-password.form/submit]]]])

(def ^:dynamic *password-chart*
  [:table.table.reset-password.password
   [:tr :?password.wrapper
    [:th :$password.label]
    [:td
     :$password.field
     :$password.help
     :$password.errors]]
   [:tr :?repeat-password.wrapper
    [:th :$repeat-password.label]
    [:td
     :$repeat-password.field
     :$repeat-password.help
     :$repeat-password.errors]]
   [:tr
    [:td [:button.btn.btn-primary :?i18n.t :reverie.batteries.objects.reset-password.form/submit]]]])

(defform email-form
  {}
  [{:name :email
    :label (delay-t :reverie.batteries.objects.reset-password.form.email/label)
    :placeholder (delay-t :reverie.batteries.objects.reset-password.form.email/placeholder)
    :validation (vlad/attr [:email] (vlad/present))}])

(defform password-form
  {}
  [{:name :password
    :type :password
    :label (delay-t :reverie.batteries.objects.reset-password.form.password/label)
    :placeholder (delay-t :reverie.batteries.objects.reset-password.form.password/placeholder)
    :opt {:min 8}
    :validation (vlad/attr [:password] (vlad/join (vlad/present)
                                                  (vlad/length-over 8)))}
   {:name :repeat-password
    :type :password
    :label (delay-t :reverie.batteries.objects.reset-password.form.repeat-password/label)
    :placeholder (delay-t :reverie.batteries.objects.reset-password.form.repeat-password/placeholder)
    :opt {:min 8}
    :validation (vlad/equals-field [:password] [:repeat-password])}])

;; renderer multi function
(def present-reset-password nil)
(defmulti present-reset-password :status)
(defmethod present-reset-password :reset [{:keys [form title description]}]
  [:div.reset-password
   (if-not (str/blank? title) [:h2 title])
   (if-not (str/blank? description) description)
   form])
(defmethod present-reset-password :reset/email-sent [{:keys [form title description]}]
  [:div.reset-password
   (if-not (str/blank? title) [:h2 title])
   (if-not (str/blank? description) description)])
(defmethod present-reset-password :reset/password-reset [{:keys [title description]}]
  [:div.reset-password
   (if-not (str/blank? title) [:h2 title])
   (if-not (str/blank? description) description)])
(defmethod present-reset-password :reset/password [{:keys [form title description]}]
  [:div.reset-password
   (if-not (str/blank? title) [:h2 title])
   (if-not (str/blank? description) description)
   form])
(defmethod present-reset-password :reset/expired-token [{:keys [title description]}]
  [:div.reset-password
   (if-not (str/blank? title) [:h2 title])
   (if-not (str/blank? description) description)])
(defmethod present-reset-password :reset/no-user [{:keys [title description]}]
  [:div.reset-password
   (if-not (str/blank? title) [:h2 title])
   (if-not (str/blank? description) description)])
(defmethod present-reset-password :default [_] nil)


;; object multi function for :get
(def reset-password nil)
(defmulti reset-password (fn [_ _ {:keys [mode]} {:keys [token reset]}]
                           (cond
                             (not (str/blank? token)) :reset/password
                             reset :reset/password-reset
                             mode mode
                             :else :reset)))
(defmethod reset-password :reset/email-sent [request object {title :title_email_sent description :description_email_sent} params]
  {:status :reset/email-sent
   :title title
   :description description})
(defmethod reset-password :reset/password-reset [request object {title :title_password_reset description :description_password_reset} params]
  {:status :reset/password-reset
   :title title
   :description description})
(defmethod reset-password :reset [request object {title :title_reset description :description_reset} params]
  {:status :reset
   :form [:form {:method "post"
                 :action ""}
          (util/anti-forgery-field)
          (form/as-flow
           *email-chart*
           (if (form/post? request)
             (email-form nil params)
             (email-form nil)))]
   :title title
   :description description})
(defmethod reset-password :reset/password [{{db :database} :reverie :as request} object properties {:keys [token] :as params}]
  (let [expired? (and (string? token) (auth/expired-token? token db))
        id (try (java.util.UUID/fromString token) (catch Exception _ nil))
        user (if id (auth/get-user db id))]
    (cond
      (nil? user)
      {:status :reset/no-user
       :title (:title_no_user properties)
       :description (:description_no_user properties)}

      (true? expired?)
      (do (do-send-email! request properties user)
          {:status :reset/expired-token
           :title (:title_expired_token properties)
           :description (:description_expired_token properties)})

      :else
      {:status :reset/password
       :title (:title_password properties)
       :description (:title_password properties)
       :form [:form {:method "post"
                     :action ""}
              (util/anti-forgery-field)
              (form/as-flow
               *password-chart*
               (if (form/post? request)
                 (password-form nil params)
                 (password-form nil)))]})))

(defmethod reset-password :default [_ _ _ _] nil)


;; object multi function for :post
(defn handle-reset-password [{uri :uri scheme :scheme headers :headers {db :database settings :settings} :reverie :as request} object {:keys [redirect_url login_p] :as properties} {:keys [token] :as params}]
  (cond
    (form/valid? (email-form nil params))
    (let [user (auth/get-user db (:email params))]
      (if user
        (do
          (do-send-email! request properties user)
          (reset-password request object (assoc properties :mode :reset/email-sent) params))

        (reset-password request object properties params)))

    (form/valid? (password-form nil params))
    (let [id (try (java.util.UUID/fromString token) (catch Exception _ nil))
          user (if id (auth/get-user db id))]
      (if user
        (do (auth/set-password! user (:password params) db)
            (auth/retire-token user db)
            (when login_p
              (auth/login user db))
            (if-not (str/blank? redirect_url)
              (redirect! redirect_url)
              (redirect! (str uri "?reset=true"))))
        (reset-password request object properties params)))

    :else
    (reset-password request object properties params)))

(defrenderer ::renderer {:render-fn :hiccup} {:any present-reset-password})

(defobject reverie/reset-password
  {:table "batteries_reset_password"
   :renderer ::renderer
   :i18n "reverie/batteries/i18n/reset-password.edn"
   :migration {:path "src/reverie/batteries/objects/migrations/reset-password"
               :table "migrations_reverie_reset_password"
               :automatic? true}
   :fields {:title {:name "Title"
                    :type :text
                    :initial ""
                    :help "Initial state title (ie, asking for email)"}
            :title_email_sent {:name "Title [Email sent]"
                               :type :text
                               :initial ""
                               :help "Title for having sent the email"}
            :title_password_reset {:name "Title [Password reset]"
                                   :type :text
                                   :initial ""
                                   :help "Title for having reset the password successfully"}
            :title_password {:name "Title [Password]"
                             :type :text
                             :initial ""
                             :help "Title for resetting the password"}
            :title_expired_token {:name "Title [Expired token]"
                                  :type :text
                                  :initial ""
                                  :help "Title for having an expired token"}
            :title_no_user {:name "Title [No user]"
                            :type :text
                            :initial ""
                            :help "Title for having found no user"}


            :description {:name "Description"
                          :type :richtext
                          :initial ""
                          :help "Description to be shown in initial state"}
            :description_email_sent {:name "Description [Email sent]"
                                     :type :richtext
                                     :initial ""
                                     :help "Description to be shown when an email has been sent"}
            :description_password {:name "Description [Password]"
                                   :type :richtext
                                   :initial ""}
            :description_password_reset {:name "Description [Password reset]"
                                         :type :richtext
                                         :initial ""}
            :description_expired_token {:name "Description [Expired token]"
                                        :type :richtext
                                        :initial ""}
            :description_no_user {:name "Description [No user]"
                                  :type :richtext
                                  :initial ""
                                  :help "Description to be shown when no user is found"}




            :email_subject {:name "Subject"
                            :type :text
                            :initial "Password reset for [site here]"}
            :email_body {:name "Email body"
                         :type :textarea
                         :initial ""
                         :help "Email body. Will be sent as both HTML and raw text. For HTML it will be wrapped in a pre-defined template."}

            :redirect_url {:name "Redirect URL"
                           :type :url
                           :initial ""
                           :help "URL to redirect to after the user has successfully reset the password. Leave blank for staying on the same page and showing the title + description for a successfully reset password"}
            :reset_url {:name "Reset URL"
                           :type :url
                           :initial ""
                           :help "URL to be used in the email. Blank for using the same page"}
            :login_p {:name "Login?"
                      :type :boolean
                      :initial false
                      :help "Do we want to login the user from a successful reset of the password?"}}
   :sections [{:name "Default" :fields [:title :description]}
              {:name "Email settings" :fields [:email_subject :email_body]}
              {:name "Email sent" :fields [:title_email_sent :description_email_sent]}
              {:name "Password" :fields [:title_password :description_password]}
              {:name "Password reset" :fields [:title_password_reset :description_password_reset]}
              {:name "Expired token" :fields [:title_expired_token :description_expired_token]}
              {:name "No user" :fields [:title_no_user :description_no_user]}

              {:name "Misc" :fields [:reset_url :redirect_url :login_p]}]}
  {:get reset-password :post handle-reset-password})
