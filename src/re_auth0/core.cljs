(ns re-auth0.core
  (:require [cljsjs.auth0]
            [re-frame.core :as re-frame]))

(defonce web-auth-instance (atom nil))

(defn *js->clj
  [js]
  (js->clj js :keywordize-keys true))

(defn make-web-auth
  [client-id domain]
  (js/auth0.WebAuth. (clj->js {:clientID client-id
                               :domain domain})))

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
