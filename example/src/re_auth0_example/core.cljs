(ns re-auth0-example.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-auth0-example.events :as events]
            [re-auth0-example.views :as views]
            [re-auth0-example.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (re-frame/dispatch [::events/initialize-web-auth])
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
