(ns reverie.batteries.objects.reset-password
  (:require [clojure.string :as str]
            [ez-form.core :refer [defform] :as form]
            [hiccup.core :refer [html]]
            [reverie.auth :as auth]
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
</style>
"]
    (email/queue-message to email_subject
                         [:alternative
                          {:type "text/plain"
                           :content body-text}
                          {:type "text/html"
                           :content (html (list css
                                                body-html))}])))
(def ^:dynamic *email-chart*
  [:table.table
   [:tr :?email.wrapper
    [:th :$email.label]
    [:td
     :$email.field
     :$email.help
     :$email.errors]]
   [:tr
    [:td [:button.btn.btn-primary (t :reverie.batteries.objects.reset-password.form/submit)]]]])

(defform email-form
  {}
  [{:name :email
    :label (t :reverie.batteries.objects.reset-password.form.email/label)
    :validation (vlad/attr [:email] (vlad/present))}])

;; renderer multi function
(def present-reset-password nil)
(defmulti present-reset-password :status)
(defmethod present-reset-password "forgot" [{:keys [url title description]}]
  [:div.forgot-password
   (if-not (str/blank? title) [:h2 title])
   (if-not (str/blank? description) description)
   [:a.btn.btn-primary {:href url} (t :reverie.batteries.objects.reset-password/forgot)]])
(defmethod present-reset-password "reset" [{:keys [form title description_reset]}]
  [:div.reset-password
   (if-not (str/blank? title) [:h2 title])
   (if-not (str/blank? description_reset) description_reset)
   form])
(defmethod present-reset-password :reset/email-sent [{:keys [form title description_email_sent]}]
  [:div.reset-password
   (if-not (str/blank? title) [:h2 title])
   (if-not (str/blank? description_email_sent) description_email_sent)])
(defmethod present-reset-password :default [_] nil)


;; object multi function for :get
(def reset-password nil)
(defmulti reset-password (fn [_ _ {:keys [mode]} _] mode))
(defmethod reset-password "forgot" [request object {:keys [mode forgot_url]} params]
  {:status mode
   :url forgot_url})
(defmethod reset-password :reset/email-sent [request object properties params]
  (merge {:status :reset/email-sent}
         (select-keys properties [:title :description_email_sent])))
(defmethod reset-password "reset" [request object {:keys [mode] :as properties} params]
  (merge {:status mode
          :form [:form {:method "post"
                        :action ""}
                 (util/anti-forgery-field)
                 (form/as-flow
                  *email-chart*
                  (if (form/post? request)
                    (email-form nil params)
                    (email-form nil)))]}
         (select-keys properties [:title :description_reset])))
(defmethod reset-password "reset-password" [{{db :database} :request} object {:keys [mode]} {:keys [token]}]
  (let [token? (auth/expired-token? token db)]
    (cond
      (not token?)
      {:status :reset-password/expired-token})))
(defmethod reset-password :default [_ _ _ _] nil)


;; object multi function for :post
(def handle-reset-password nil)
(defmulti handle-reset-password (fn [_ _ {:keys [mode]} _] mode))
(defmethod handle-reset-password "reset" [{uri :uri scheme :scheme headers :headers {db :database settings :settings} :reverie :as request} object {:keys [reset_url] :as properties} params]
  (if (form/valid? (email-form nil params))

    (let [user (auth/get-user db (:email params))]
      (if user

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
                              :to (:email params)
                              :url url
                              :token token))
          (reset-password request object (assoc properties :mode :reset/email-sent) params))

        (reset-password request object properties params)))

    (reset-password request object properties params)))
(defmethod handle-reset-password :default [request object properties params]
  (reset-password request object properties params))

(defrenderer ::renderer {:render-fn :hiccup} {:any present-reset-password})

(defobject reverie/reset-password
  {:table "batteries_reset_password"
   :renderer ::renderer
   :i18n "reverie/batteries/i18n/reset-password.edn"
   :migration {:path "src/reverie/batteries/objects/migrations/reset-password"
               :table "migrations_reverie_reset_password"
               :automatic? true}
   :fields {:mode {:name "Mode"
                   :type :dropdown
                   :options [["Forgot your password?" "forgot"]
                             ["Reset password" "reset"]
                             ["Handle reset password" "reset-password"]]
                   :initial "reset"}
            :title {:name "Title"
                    :type :text
                    :initial ""}
            :email_subject {:name "Subject"
                            :type :text
                            :initial "Password reset for [site here]"}
            :email_body {:name "Email body"
                         :type :textarea
                         :initial ""
                         :help "Email body. Will be sent as both HTML and raw text. For HTML it will be wrapped in a pre-defined template."}
            :description {:name "Description (Reset password / Forgot your password?)"
                          :type :richtext
                          :initial ""
                          :help "Description to be shown when mode 'Reset password' or 'Forgot your password?' is active"}
            :description_no_user {:name "Description (No user)"
                                  :type :richtext
                                  :initial ""
                                  :help "Description to be shown when no user is found"}
            :description_email_sent {:name "Description (Email sent)"
                                     :type :richtext
                                     :initial ""
                                     :help "Description to be shown when an email has been sent from mode 'Reset password'"}
            :description_handle_reset {:name "Description (Handle reset password)"
                                       :type :richtext
                                       :initial ""
                                       :help "Description to be shown when mode 'Handle reset password' is active"}
            :forgot_url {:name "URL for [Forgot your password?]"
                         :type :url
                         :initial ""
                         :help "Which page to point to for resetting your password?"}
            :reset_url {:name "URL for details on [Handle reset password]"
                        :type :url
                        :initial ""
                        :help "URL for where you land after a link has been sent out to the email you filled in."}
            :redirect_url {:name "URL for a successful [Handle reset password]"
                           :type :url
                           :initial ""
                           :help "URL to redirect to after the user has successfully reset the password"}}
   :sections [{:fields [:mode]}
              {:name "Texts" :fields [:title :description :description_no_user :description_email_sent :description_handle_reset]}
              {:name "URLs" :fields [:forgot_url :reset_url :redirect_url]}]}
  {:get reset-password :post handle-reset-password})
