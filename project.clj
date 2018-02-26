(defproject lifecheq/re-auth0 "0.1.0-SNAPSHOT"
  :description "re-frame effects for Auth0"
  :url "https://github.com/lifecheq/re-auth0"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [cljsjs/auth0 "9.2.1-0"]
                 [re-frame "0.10.4"]]
  :min-lein-version "2.7.1"
  :cljsbuild {:builds {}} ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413
  :source-paths ["src" "target/classes"]
  :clean-targets ["out" "release"]
  :target-path "target")
