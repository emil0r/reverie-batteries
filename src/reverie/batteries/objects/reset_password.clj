(ns reverie.batteries.objects.reset-password
  (:require [clojure.string :as str]
            [ez-form.core :refer [defform] :as form]
            [reverie.auth :as auth]
            [reverie.core :refer [defobject defrenderer]]
            [reverie.i18n :refer [t]]
            [reverie.util :refer [redirect!]]
            [vlad.core :as vlad]))


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

(def present-reset-password nil)
(defmulti present-reset-password :status)
(defmethod present-reset-password "forgot" [{:keys [url title description]}]
  [:div.forgot-password
   (if-not (str/blank? title) [:h2 title])
   (if-not (str/blank? description) [:p description])
   [:a.btn.btn-primary {:href url} (t :reverie.batteries.objects.reset-password/forgot)]])
(defmethod present-reset-password "reset" [{:keys [form title description_reset]}]
  [:div.reset-password
   (if-not (str/blank? title) [:h2 title])
   (if-not (str/blank? description_reset) [:p description_reset])
   (form/as-flow *email-chart* form)])
(defmethod present-reset-password :reset/email-sent [{:keys [form title description_email_sent]}]
  [:div.reset-password
   (if-not (str/blank? title) [:h2 title])
   (if-not (str/blank? description_email_sent) [:p description_email_sent])])
(defmethod present-reset-password :default nil)

(def reset-password nil)
(defmulti reset-password (fn [_ _ {:keys [mode]} _] mode))
(defmethod reset-password "forgot" [request object {:keys [forgot_url]} params]
  {:status mode
   :url forgot_url})
(defmethod reset-password "reset" [request objects properties {:keys [email-sent?] :as params}]
  (cond
    email-sent?
    (merge {:status :reset/email-sent}
           (select-keys properties [:title :description_email_sent]))

    :else
    (merge {:status mode
            :form (form/as-flow
                   *email-chart*
                   (if (form/post? request)
                     (email-form nil params)
                     (email-form params)))}
           (select-keys properties [:title :description_reset]))))
(defmethod reset-password :default [_ _ _ _] nil)

(def handle-reset-password nil)
(defmulti handle-reset-password (fn [_ _ {:keys [mode]} _] mode))
(defmethod handle-reset-password "reset" [request object properties params]
  (if (form/valid? (email-form nil params))
    (do
      nil ;; send email
      ;; have it a multi method that send in the default method
      ;; but the dispatch being sent in is an undefined method
      ;; which would allow for an override
      (reset-password request object properties (assoc params :email-sent? true)))
    (reset-password request object properties params)))
(defmethod handle-reset-password :default [request object properties params]
  (reset-password request object properties params))

(defrenderer ::renderer {:render-fn :hiccup} {:get present-reset-password})

(defobject reverie/reset-password
  {:table "batteries_reset_password"
   :i18n "resources/reverie/batteries/i18n/reset-password.edn"
   :migration {:path "src/reverie/batteries/objects/migrations/reset-password"
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
            :description {:name "Description (Reset password / Forgot your password?)"
                          :type :richtext
                          :initial "Description to be shown when mode 'Reset password' or 'Forgot your password?' is active"}
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
                         :initial ""}
            :redirect_url {:name "URL for [Handle reset password]"
                           :type :url
                           :initial ""
                           :help "URL to redirect to after the user has successfully reset the password"}}
   :sections [{:fields [:mode :title :description_reset :description_email_sent :description_handle_reset :forgot_url]}]}
  {:get reset-password :post handle-reset-password})
