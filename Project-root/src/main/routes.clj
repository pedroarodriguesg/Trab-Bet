(ns main.routes
  (:require [io.pedestal.http.route :as route]
            [main.handlers :as handlers]))

(def routes
  (route/expand-routes
    #{["/esportes" :get handlers/listar-esportes :route-name :listar-esportes]
      ["/esportes/:sport-id/schedules" :get handlers/listar-schedules :route-name :listar-schedules]
      ["/games/:game-id/odds" :get handlers/get-odds-handler :route-name :get-odds]
      ["/apostar" :post handlers/realizar-aposta :route-name :realizar-aposta]
      ["/saldo/apostar" :post handlers/conferencia-saldo-aposta :route-name :conferencia-saldo-aposta]}))
