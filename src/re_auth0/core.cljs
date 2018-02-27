(ns re-auth0.core
  (:require [cljsjs.auth0]
            [clojure.walk :as walk]
            [re-frame.core :as re-frame]))

(defonce web-auth-instance (atom nil))

(defn *js->clj
  "Always keywordize keys"
  [js]
  (js->clj js :keywordize-keys true))

(defn remove-nils-and-empty
  "Take a map, and remove all nil or empty values"
  [m]
  (let [f (fn [[k v]]
            (when-not (or (nil? v) (empty? v))
              [k v]))]
    (walk/postwalk (fn [x]
                     (if (map? x)
                       (into {} (map f x))
                       x))
                   m)))

(defn web-auth
  "Builds the WebAuth object."
  [{:keys [domain client-id redirect-uri scope
           audience response-type response-mode]}]
  (js/auth0.WebAuth. (clj->js
                      (remove-nils-and-empty
                       {:domain       domain
                        :clientID     client-id
                        :redirectUri  redirect-uri
                        :scope        scope
                        :audience     audience
                        :responseType response-type
                        :responseMode response-mode}))))

(defn make-web-auth
  [client-id domain]
  (web-auth {:domain    domain
             :client-id client-id}))

(defn auth-results-cb
  [on-auth-result on-error]
  (fn [err auth-result]
    (if err
      (re-frame/dispatch (conj on-error (*js->clj err)))
      (re-frame/dispatch (conj on-auth-result
                               (*js->clj auth-result))))))

(defn parse-hash
  [web-auth {:keys [hash state nonce]}
   on-authenticated on-error]
  (.parseHash web-auth (clj->js
                        (remove-nils-and-empty
                         {:hash hash
                          :state state
                          :nonce nonce}))
              (auth-results-cb on-authenticated
                               on-error))
  (set! (.-hash js/window.location) ""))

(defn authorize
  "The basic authorize"
  [web-auth {:keys [audience connection scope response-type
                    client-id redirect-uri leeway state]}
   on-authenticated on-error]
  (.authorize web-auth (clj->js
                        (remove-nils-and-empty
                         {:audience     audience
                          :connection   connection
                          :scope        scope
                          :responseType response-type
                          :clientID     client-id
                          :redirectUri  redirect-uri
                          :leeway       leeway
                          :state        state}))
              (auth-results-cb on-authenticated
                               on-error)))

(defn logout
  "Logout"
  [web-auth {:keys [return-to client-id]}]
  (.logout web-auth (clj->js
                     (remove-nils-and-empty
                      {:returnTo return-to
                       :clientID client-id}))))

(defn check-session
  "Check session"
  [web-auth {:keys [domain client-id response-type state nonce
                    redirect-uri scope audience timeout]}
   on-authenticated on-error]
  (.checkSession web-auth (clj->js
                           (remove-nils-and-empty
                            {:domain       domain
                             :clientID     client-id
                             :responseType response-type
                             :state        state
                             :nonce        nonce
                             :redirectUri  redirect-uri
                             :scope        scope
                             :audience     audience
                             :timeout      timeout}))
                 (auth-results-cb on-authenticated
                                  on-error)))

(defn web-auth-fx
  [value]
  (let [web-auth @web-auth-instance]
    (case (:method value)
      :init          (reset! web-auth-instance
                             (make-web-auth (:client-id value)
                                            (:domain value)))
      :authorize     (authorize web-auth
                                (:options value)
                                (:on-authenticated value)
                                (:on-error value))
      :parse-hash    (parse-hash web-auth
                                 (:options value)
                                 (:on-authenticated value)
                                 (:on-error value))
      :logout        (.logout web-auth
                              (clj->js {:returnTo (:return-to value)
                                        :clientID (:client-id value)}))
      :check-session (check-session web-auth
                                    (:options value)
                                    (:on-authenticated value)
                                    (:on-error value))
      nil)))

(re-frame/reg-fx
 ::web-auth
 web-auth-fx)

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
 ::parse-hash
 (fn [{:keys [on-authenticated on-error] :as options}]
   (parse-hash @web-auth-instance
               options
               on-authenticated
               on-error)))

(re-frame/reg-fx
 ::logout
 (fn [options]
   (logout @web-auth-instance
           options)))

(re-frame/reg-fx
 ::check-session
 (fn [{:keys [on-authenticated on-error] :as options}]
   (check-session @web-auth-instance
                  options
                  on-authenticated
                  on-error)))
