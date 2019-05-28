(defproject globar "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [com.walmartlabs/lacinia "0.32.0"]
                 [com.walmartlabs/lacinia-pedestal "0.11.0"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [org.postgresql/postgresql "42.2.5.jre7"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [io.aviso/logging "0.3.1"]
                 [mount "0.1.16"]
                 [clojure.java-time "0.3.2"]
                 [org.threeten/threeten-extra "1.4"]
                 [io.pedestal/pedestal.jetty "0.5.5"]
                 [fipp "0.6.18"]
                 [com.auth0/auth0 "1.13.2"]
                 [com.auth0/java-jwt "3.8.1"]
                 [buddy/buddy-sign "2.2.0"]]
  :main globar.core
  :aot [globar.core]
  :resource-paths ["dev-resources" "resources"])
