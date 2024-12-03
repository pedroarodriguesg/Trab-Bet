(ns main.handlers
  (:require [main.utils :as utils]
            [cheshire.core :as json]
            [main.db :as db]
            [main.saldo :as saldo])) 

(defn listar-esportes
  "Lista esportes disponíveis entre IDs 4 e 19 e abre o menu de seleção."
  [request]
  (try
    (let [response (utils/fetch-from-api "/sports" {})
          esportes (when (:sports response)
                     (->> (:sports response)
                          (filter #(and (integer? (:sport_id %)) 
                                        (>= (:sport_id %) 4)
                                        (<= (:sport_id %) 19)))
                          (map #(select-keys % [:sport_id :sport_name]))))]
      (if (seq esportes)
        (do
          (println "\n--- ESPORTES DISPONIVEIS ---")
          (doseq [[idx esporte] (map-indexed vector esportes)]
            (println (str (inc idx) ". " (:sport_name esporte))))
          (println "Escolha o numero do esporte:")
          (let [escolha (read-line)]
            (try
              (let [indice (Integer/parseInt escolha)
                    esporte-selecionado (nth esportes (dec indice))]
                (println (str "Voce escolheu: " (:sport_name esporte-selecionado)))
                (:sport_id esporte-selecionado))
              (catch Exception _ (println "Opção invalida! Tente novamente.")))))
        (println "Nenhum esporte encontrado no filtro de IDs (4-19).")))
    (catch Exception e
      (println "Erro ao listar esportes:" (.getMessage e)))))

(defn get-odds-handler
  [request]
  (let [game-id (get-in request [:path-params :game-id])
        odds (utils/fetch-odds game-id)]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (json/generate-string odds)}))

(defn listar-schedules
  "Lista o cronograma de eventos para um esporte específico."
  [request]
  (try
    (let [sport-id (get-in request [:path-params :sport-id])
          response (utils/fetch-from-api (str "/sports/" sport-id "/schedule") {:limit "100"})
          schedules (:schedules response)]
      (if (seq schedules)
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:schedules schedules})}
        {:status 404
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:mensagem "Nenhum cronograma encontrado para o esporte selecionado."})}))
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:mensagem "Erro ao listar cronogramas."
                                    :erro (.getMessage e)})})))

(defn consultar-saldo-handler
  [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string {:saldo (saldo/consultar-saldo)})})

(defn atualizar-saldo-handler
  [request]
  (try
    (let [valor (get-in request [:json-params :valor])]
      (if valor
        (let [resultado (saldo/atualizar-saldo valor)]
          {:status (if (:erro resultado) 400 200)
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string (if (:erro resultado)
                                         {:mensagem (:erro resultado)}
                                         {:mensagem "Saldo atualizado com sucesso!"
                                          :novo-saldo (:valor @saldo/saldo)}))})
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:mensagem "Valor inválido para atualizar saldo"})}))
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:mensagem "Erro ao atualizar saldo."
                                    :erro (.getMessage e)})})))

(defn conferencia-saldo-aposta
  "Confere e debita o valor do saldo para realizar uma aposta."
  [request]
  (try
    (let [dados (:json-params request)
          valor (:valor dados)]
      (if valor
        (let [resultado (saldo/realizar-aposta valor)]
          {:status (if (:erro resultado) 400 200)
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string resultado)})
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:mensagem "Valor inválido para realizar aposta"})}))
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:mensagem "Erro ao realizar aposta."
                                    :erro (.getMessage e)})})))

(defn realizar-aposta
  "Permite realizar uma aposta em um evento específico."
  [request]
  (try
    (let [dados (:json-params request)
          event-id (:event_id dados)
          selecao (:selecao dados)
          valor (:valor dados)
          schedules (:schedules (utils/fetch-schedules (:sport_id dados)))]
      (if-let [evento (some #(when (= (:event_id %) event-id) %) schedules)]
        (let [aposta {:event_id event-id
                      :event_name (:event_name evento)
                      :selecao selecao
                      :valor valor
                      :data_evento (:date_event evento)}]
          (db/salvar-aposta aposta)
          {:status 200
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:mensagem "Aposta realizada com sucesso!"
                                        :aposta aposta})})
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:mensagem "Evento não encontrado ou inválido."})}))
    (catch Exception e
      {:status 500
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string {:mensagem "Erro ao processar aposta."
                                    :erro (.getMessage e)})})))