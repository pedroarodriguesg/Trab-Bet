(ns main.service
  (:require [io.pedestal.http :as http]
            [main.routes :refer [routes]]))

(def service
  {:env :prod
   ::http/routes routes
   ::http/resource-path "/public"
   ::http/type :jetty
   ::http/port 8080})

(defn start
  "Inicia o servidor HTTP com o Pedestal."
  []
  (http/start (http/create-server service)))
