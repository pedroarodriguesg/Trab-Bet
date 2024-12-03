(ns main.routes
  (:require [io.pedestal.http.route :as route]
            [main.handlers :as handlers]))

(def routes
  (route/expand-routes
    #{["/esportes" :get handlers/listar-esportes :route-name :listar-esportes]
      ["/esportes/:sport-id/schedules" :get handlers/listar-schedules :route-name :listar-schedules]
      ["/events/:event-id" :get handlers/get-event-handler :route-name :get-event]
      ["/apostar" :post handlers/realizar-aposta :route-name :realizar-aposta]
      ["/saldo/apostar" :post handlers/conferencia-saldo-aposta :route-name :conferencia-saldo-aposta]}))
