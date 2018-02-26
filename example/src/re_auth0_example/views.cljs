(ns re-auth0-example.views
  (:require [re-frame.core :as re-frame]
            [re-auth0-example.subs :as subs]
            [re-auth0-example.events :as events]))

(defn button
  [event-vec label]
  [:button.btn.btn-primary
   {:on-click #(re-frame/dispatch event-vec)}
   label])

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div.container
     [:h1 [:code "re-auth0"] " playground"]
     [:p "This parses the hash fragment, looking for auth results"]
     [:button.btn.btn-primary
      {:on-click #(re-frame/dispatch [::events/parse-hash])}
      "Parse hash"]
     [:h3 "Login with /authorize"]
     [:button.btn.btn-primary
      {:on-click #(re-frame/dispatch [::events/login-hosted])}
      "Hosted login"]
     [:button.btn.btn-primary
      {:on-click #(re-frame/dispatch [::events/login-facebook])}
      "Facebook login"]
     [:h3 "Check if you have an active session"]
     [button [::events/check-session] "Check session"]]))
