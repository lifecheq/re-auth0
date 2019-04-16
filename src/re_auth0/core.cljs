(ns re-auth0.core
  (:require [cljsjs.auth0]
            [re-frame.core :as re-frame]))


(defonce web-auth-instance (atom nil))


(defonce app-state (atom {}))


(defn local-storage-key []
  (str "auth0."
       (-> @app-state :info :client-id)))


(defn *js->clj
  "Always keywordize keys"
  [js]
  (js->clj js :keywordize-keys true))


(defn remove-nils-and-empty
  "Take a map, and remove all nil or empty values"
  [m]
  (->> m
       (filter (fn [[k v]]
                 (not (or (nil? v) (empty? v)))))
       (into {})))


(defn *clj->js
  "Nil-prune too"
  [clj]
  (clj->js (remove-nils-and-empty clj)))


(defn web-auth
  "Builds the WebAuth object."
  [{:keys [domain client-id redirect-uri scope
           audience response-type response-mode]}]
  (js/auth0.WebAuth. (*clj->js
                      {:domain       domain
                       :clientID     client-id
                       :redirectUri  redirect-uri
                       :scope        scope
                       :audience     audience
                       :responseType response-type
                       :responseMode response-mode})))


(defn auth-results-cb
  [on-auth-result on-error]
  (fn [err auth-result]
    (swap! app-state dissoc :popup-handler)
    (if err
      (if on-error
        (re-frame/dispatch (conj on-error (*js->clj err)))
        (when-let [defautl-on-error (:on-error @app-state)]
          (re-frame/dispatch (conj defautl-on-error (*js->clj err)))))
      (do
        (.setItem (.-localStorage js/window)
                  (local-storage-key)
                  (.stringify js/JSON auth-result))
        (if on-auth-result
          (re-frame/dispatch (conj on-auth-result
                                   (*js->clj auth-result)))
          (when-let [default-on-auth-result (:on-authenticated @app-state)]
            (re-frame/dispatch (conj default-on-auth-result
                                     (*js->clj auth-result)))))))))


(defn parse-hash
  [web-auth {:keys [hash state nonce]}
   on-authenticated on-error]
  (.parseHash web-auth (*clj->js
                        {:hash hash
                         :state state
                         :nonce nonce})
              (auth-results-cb on-authenticated
                               on-error))
  (set! (.-hash js/window.location) ""))


(defn authorize
  "The basic authorize"
  [web-auth {:keys [audience connection scope response-type
                    client-id redirect-uri leeway state]}
   on-authenticated on-error]
  (.authorize web-auth (*clj->js
                        {:audience     audience
                         :connection   connection
                         :scope        scope
                         :responseType response-type
                         :clientID     client-id
                         :redirectUri  redirect-uri
                         :leeway       leeway
                         :state        state})
              (auth-results-cb on-authenticated
                               on-error)))


(defn popup-preload
  "Preloads the popup window, to get around blockers"
  []
  (if (:popup-handler @app-state)
    (js/console.warn "Popup handler already exists. Not creating another.")
    (swap! app-state
           assoc :popup-handler
           (.preload (.-popup @web-auth-instance)))))


(defn popup-authorize
  "Popup variant"
  [web-auth {:keys [audience connection scope response-type
                    client-id redirect-uri leeway state]}
   on-authenticated on-error]
  (.authorize (.-popup web-auth)
              (let [opts          (*clj->js {:audience     audience
                                             :connection   connection
                                             :scope        scope
                                             :responseType response-type
                                             :clientID     client-id
                                             :redirectUri  redirect-uri
                                             :leeway       leeway
                                             :state        state})
                    popup-handler (:popup-handler @app-state)]
                (when popup-handler
                  (aset opts "popupHandler" popup-handler))
                opts)
              (auth-results-cb on-authenticated
                               on-error)))


(defn logout
  "Logout"
  [web-auth {:keys [return-to client-id]}]
  (.logout web-auth
           (*clj->js {:returnTo return-to
                      :clientID client-id})))


(defn check-session
  "Check session"
  [web-auth {:keys [domain client-id response-type state nonce
                    redirect-uri scope audience timeout]}
   on-authenticated on-error]
  (.checkSession web-auth (*clj->js
                           {:domain       domain
                            :clientID     client-id
                            :responseType response-type
                            :state        state
                            :nonce        nonce
                            :redirectUri  redirect-uri
                            :scope        scope
                            :audience     audience
                            :timeout      timeout})
                 (auth-results-cb on-authenticated
                                  on-error)))


(defn passwordless-start
  "Start passwordless authentication"
  [web-auth {:keys [connection send phone-number email]}
   on-authenticated on-error]
  (.passwordlessStart web-auth (*clj->js
                                {:connection  connection
                                 :send        send
                                 :email       email
                                 :phoneNumber phone-number})
                      (auth-results-cb on-authenticated
                                       on-error)))


(defn passwordless-login
  "Enter code for passwordless login"
  [web-auth {:keys [connection code phone-number email]}
   on-authenticated on-error]
  (.passwordlessLogin web-auth (*clj->js
                                {:connection       connection
                                 :verificationCode code
                                 :email            email
                                 :phoneNumber      phone-number})
                      (auth-results-cb on-authenticated
                                       on-error)))


(defn dispatch-error
  [on-error err]
  (if on-error
    (re-frame/dispatch (conj on-error err))
    (when-let [default-on-error (:on-error @app-state)]
      (re-frame/dispatch (conj default-on-error err)))))


(defn signup
  "Signs up using username password"
  [web-auth {:keys [username email password connection metadata]}
   on-success on-error]
  (.signup web-auth (*clj->js
                     {:username      username
                      :email         email
                      :password      password
                      :connection    connection
                      :user_metadata metadata})
           (fn [err]
             (if err
               (dispatch-error on-error err)
               (re-frame/dispatch on-success)))))


(defn login
  "Logs in user username and password"
  [web-auth {:keys [username email password connection]}
   on-authenticated on-error]
  (.login web-auth (*clj->js
                    {:username username
                     :email    email
                     :password password
                     :realm    connection})
          (auth-results-cb on-authenticated
                           on-error)))


(defn reset-password
  "Requests a password reset"
  [web-auth {:keys [email connection]}
   on-success on-error]
  (.changePassword web-auth (*clj->js
                             {:email      email
                              :connection connection})
                   (fn [err resp]
                     (if err
                       (dispatch-error on-error err)
                       (re-frame/dispatch (conj on-success resp))))))


;; App boot fn

(defn maybe-auth-result
  "Tries to fetch auth results from local storage"
  []
  (when-let [v (.getItem (.-localStorage js/window)
                         (local-storage-key))]
    (*js->clj (.parse js/JSON v))))


(defn init-app
  "A more substantial app boot, including loading stored credentials"
  [auth0-info {:keys [on-authenticated on-error]}]
  (swap! app-state assoc :info auth0-info)
  (when on-authenticated
    (swap! app-state assoc :on-authenticated on-authenticated))
  (when on-error
    (swap! app-state assoc :on-error on-error))
  (reset! web-auth-instance
          (web-auth auth0-info))
  (let [hash   (-> js/window .-location .-hash)
        stored (maybe-auth-result)]
    (cond
      ;; Hash fragment in the URL
      (> (count hash) 100)
      (parse-hash @web-auth-instance
                  nil
                  on-authenticated
                  on-error)
      ;; Local storage
      stored
      (re-frame/dispatch (conj on-authenticated stored)))))


;; Registering re-frame effects


(re-frame/reg-fx
 ::init
 (fn [options]
   (reset! web-auth-instance (web-auth options))))


(re-frame/reg-fx
 ::authorize
 (fn [{:keys [on-authenticated on-error] :as options}]
   (authorize @web-auth-instance
              options
              on-authenticated
              on-error)))


(re-frame/reg-fx
 ::popup-authorize
 (fn [{:keys [on-authenticated on-error] :as options}]
   (popup-authorize @web-auth-instance
                    options
                    on-authenticated
                    on-error)))


(re-frame/reg-fx
 ::parse-hash
 (fn [{:keys [on-authenticated on-error] :as options}]
   (parse-hash @web-auth-instance
               options
               on-authenticated
               on-error)))


(re-frame/reg-fx
 ::logout
 (fn [options]
   (.removeItem (.-localStorage js/window)
                (local-storage-key))
   (logout @web-auth-instance
           (merge {:client-id (:client-id @app-state)}
                  options))))


(re-frame/reg-fx
 ::check-session
 (fn [{:keys [on-authenticated on-error] :as options}]
   (check-session @web-auth-instance
                  options
                  on-authenticated
                  on-error)))


(re-frame/reg-fx
 ::passwordless-start
 (fn [{:keys [on-authenticated on-error] :as options}]
   (passwordless-start @web-auth-instance
                       options
                       on-authenticated
                       on-error)))


(re-frame/reg-fx
 ::passwordless-login
 (fn [{:keys [on-authenticated on-error] :as options}]
   (passwordless-login @web-auth-instance
                       options
                       on-authenticated
                       on-error)))


(re-frame/reg-fx
 ::signup
 (fn [{:keys [on-success on-error] :as options}]
   (signup @web-auth-instance
           options
           on-success
           on-error)))


(re-frame/reg-fx
 ::login
 (fn [{:keys [on-authenticated on-error] :as options}]
   (login @web-auth-instance
          options
          on-authenticated
          on-error)))


(re-frame/reg-fx
 ::reset-password
 (fn [{:keys [on-success on-error] :as options}]
   (reset-password @web-auth-instance
                   options
                   on-success
                   on-error)))
