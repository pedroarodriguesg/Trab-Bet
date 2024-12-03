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

(defn fetch-odds
  "Busca as odds de um evento usando a API de eventos."
  [event-id]
  (try
    (let [url (str base-url "/events/" event-id)
          headers {"x-rapidapi-key" api-key
                   "x-rapidapi-host" "therundown-therundown-v1.p.rapidapi.com"}
          params {:headers headers
                  :query-params {:include "lines"}
                  :as :json}
          response (client/get url params)]
      (if (= 200 (:status response))
        (let [event (-> response :body)
              lines (get-in event [:lines])
              first-line (first (vals lines))]
          {:moneyline (:moneyline first-line)
           :spread (:spread first-line)})
        (do
          (println "Erro ao buscar odds: Código de status" (:status response))
          {}))) 
    (catch Exception e
      (println "Erro ao buscar odds para o evento" event-id ":" (.getMessage e))
      {}))) 

(defn fetch-event-result
  "Busca o resultado do evento na API externa."
  [event-id]
  (try
    (let [url (str base-url "/events/" event-id)
          headers {"x-rapidapi-key" api-key
                   "x-rapidapi-host" "therundown-therundown-v1.p.rapidapi.com"}
          params {:headers headers
                  :query-params {:include "scores"}
                  :as :json}
          response (client/get url params)]
      (if (= 200 (:status response))
        (let [event (-> response :body)
              winner-home (get-in event [:score :winner_home])
              winner-away (get-in event [:score :winner_away])
              home-score (get-in event [:score :score_home])
              away-score (get-in event [:score :score_away])]
          {:winner (cond
                     (= 1 winner-home) "Home"
                     (= 1 winner-away) "Away"
                     :else "Draw")
           :home-score home-score
           :away-score away-score})
        (do
          (println "Erro ao buscar resultado do evento. Código de status:" (:status response))
          nil)))
    (catch Exception e
      (println "Erro ao buscar resultado do evento:" (.getMessage e))
      nil)))

(defn fetch-schedules
  "Busca o cronograma de eventos para um esporte específico."
  [sport-id]
  (fetch-from-api (str "/sports/" sport-id "/schedule") {:limit "100"}))

(defn validar-esporte [sport-id]
  "Valida se o esporte existe."
  (some #(= sport-id (:id %)) (fetch-from-api "/sports" {})))

(defn validar-jogo [sport-id evento-id selecao]
  "Valida se o jogo e seleção são válidos."
  (let [response (fetch-from-api (str "/sports/" sport-id "/dates") {:format "date" :offset "0"})]
    (some #(and (= (:id %) evento-id) (= (:selection %) selecao)) (:events response))))