(ns re-auth0-example.events
  (:require [re-frame.core :as re-frame]
            [re-auth0.core :as re-auth0]
            [re-auth0-example.db :as db]))

(re-frame/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::print
 (fn [db [_ data]]
   (.log js/console data)
   (assoc db :console data)))

(re-frame/reg-event-fx
 ::initialize-web-auth
 (fn [_ _]
   {::re-auth0/init {:domain        "brucke.auth0.com"
                     :client-id     "k5u3o2fiAA8XweXEEX604KCwCjzjtMU6"
                     :response-type "token"
                     :redirect-uri  "http://localhost:3000"}}))

(re-frame/reg-event-fx
 ::parse-hash
 (fn [_ _]
   {::re-auth0/parse-hash {:on-authenticated [::print]
                           :on-error         [::print]}}))

(re-frame/reg-event-fx
 ::login-hosted
 (fn [_ _]
   {::re-auth0/authorize {}}))

(re-frame/reg-event-fx
 ::login-facebook
 (fn [_ _]
   {::re-auth0/authorize {:connection "facebook"}}))

(re-frame/reg-event-fx
 ::check-session
 (fn [_ _]
   {::re-auth0/check-session {:connection       "facebook"
                              :on-authenticated [::print]
                              :on-error         [::print]}}))

(re-frame/reg-event-fx
 ::logout
 (fn [_ _]
   {::re-auth0/logout {:return-to "http://localhost:3000"}}))
