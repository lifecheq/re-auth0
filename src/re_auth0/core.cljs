(ns re-auth0.core
  (:require [cljsjs.auth0]
            [clojure.walk :as walk]
            [re-frame.core :as re-frame]))

(defonce web-auth-instance (atom nil))

(defn *js->clj
  [js]
  (js->clj js :keywordize-keys true))

(defn remove-nils-and-empty
  "Take a map, and remove all nil or empty values"
  [m]
  (let [f (fn [[k v]] (when (not (or (nil? v)
                                     (empty? v)))
                        [k v]))]
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(defn web-auth
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
  (js/auth0.WebAuth. (clj->js {:clientID client-id
                               :domain domain}))
  (web-auth {:domain domain
             :client-id client-id}))

(defn auth-results-cb
  [on-auth-result on-error]
  (fn [err auth-result]
    (if err
      (re-frame/dispatch (conj on-error (*js->clj err)))
      (re-frame/dispatch (conj on-auth-result
                               (*js->clj auth-result))))))

(defn parse-hash
  [web-auth opts on-auth-result on-error]
  (.parseHash web-auth (clj->js opts)
              (auth-results-cb on-auth-result on-error))
  (set! (.-hash js/window.location) ""))

(defn login
  [web-auth opts on-auth-result on-error]
  (.authorize web-auth (clj->js opts)
              (auth-results-cb on-auth-result on-error)))

(defn check-session
  [web-auth opts on-auth-result on-error]
  (.checkSession web-auth (clj->js opts)
                 (auth-results-cb on-auth-result on-error)))

(defn web-auth-fx
  [value]
  (let [web-auth @web-auth-instance]
    (case (:method value)
      :init          (reset! web-auth-instance
                             (make-web-auth (:client-id value)
                                            (:domain value)))
      :authorize     (login web-auth
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
