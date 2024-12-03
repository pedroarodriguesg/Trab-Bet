(defproject trabalho-bet "0.1.0-SNAPSHOT"
  :description "Um projeto Clojure para integração com a API theRunDown"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [io.pedestal/pedestal.service "0.5.10"]
                 [io.pedestal/pedestal.jetty "0.5.10"]
                 [clj-http "3.12.3"]
                 [cheshire "5.10.1"]
                 [com.novemberain/monger "3.5.0"]]
  :main ^:skip-aot main.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
