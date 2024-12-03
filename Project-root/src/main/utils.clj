 (ns main.utils
  (:require [clj-http.client :as client]
            [cheshire.core :as json])) 

(def api-key "449901eb4bmshe650fd7c9160be6p1d868cjsnca6328c5a3fb")
(def base-url "https://therundown-therundown-v1.p.rapidapi.com")

(defn fetch-from-api
  "Faz uma chamada GET à API externa e retorna os dados em formato JSON."
  [endpoint query-params]
  (try
    (let [url (str base-url endpoint)
          response (client/get url {:headers {"x-rapidapi-key" api-key
                                              "x-rapidapi-host" "therundown-therundown-v1.p.rapidapi.com"}
                                    :query-params query-params
                                    :as :json})]
      ;;(println "Resposta da API:" response) 
      (if (= 200 (:status response))
        (:body response)
        (do
          (println "Erro na API: Código de status:" (:status response))
          nil)))
    (catch Exception e
      (println "Erro ao acessar a API externa:" (.getMessage e))
      nil)))

(defn fetch-odds [event-id]
  (try
    (let [url (str base-url "/events/" event-id)
          headers {"x-rapidapi-key" api-key
                   "x-rapidapi-host" "therundown-therundown-v1.p.rapidapi.com"}
          params {:headers headers
                  :query-params {:include "all_periods,lines"}
                  :as :json}
          response (client/get url params)
          status (:status response)]
      (if (= status 200)
        ;; Extrair as odds da resposta
        (let [event (-> response :body :events first)
              lines (get-in event [:lines])
              first-line (first (vals lines))
              moneyline (:moneyline first-line)]
          (println "Odds obtidas para o evento" event-id ":" moneyline)
          moneyline)
        (do
          (println "Erro ao buscar odds: Código de status" status)
          nil)))
    (catch Exception e
      (println "Erro ao buscar odds para o evento" event-id ":" (.getMessage e))
      nil)))

;; Função fetch-schedules
(defn fetch-schedules
  "Busca o cronograma de eventos para um esporte específico."
  [sport-id]
  (fetch-from-api (str "/sports/" sport-id "/schedule") {:limit "100"}))


;; Validação de esporte
(defn validar-esporte [sport-id]
  "Valida se o esporte existe."
  (some #(= sport-id (:id %)) (fetch-from-api "/sports" {})))

;; Validação de jogo
(defn validar-jogo [sport-id evento-id selecao]
  "Valida se o jogo e seleção são válidos."
  (let [response (fetch-from-api (str "/sports/" sport-id "/dates") {:format "date" :offset "0"})]
    (some #(and (= (:id %) evento-id) (= (:selection %) selecao)) (:events response))))