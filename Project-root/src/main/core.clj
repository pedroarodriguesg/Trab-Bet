 (ns main.core
  (:require [main.service :as service]
            [main.saldo :as saldo]
            [main.utils :as utils]
            [main.handlers :as handlers]
            [clojure.pprint :refer [pprint]]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(def apostas (atom []))
(def selected-sport-id (atom nil))
(def selected-date (atom nil))
(def filtered-games (atom []))

(defn mostrar-menu []
  (println "\n--- MENU PRINCIPAL ---")
  (println "1. Adicionar Saldo")
  (println "2. Consultar Saldo")
  (println "3. Escolher Esporte")
  (println "4. Filtrar Jogos por Data")
  (println "5. Realizar Aposta")
  (println "6. Consultar Apostas Realizadas")
  (println "7. Liquidar Aposta")
  (println "8. Sair")
  (println "Escolha uma opcao:"))

(defn adicionar-saldo []
  (println "Digite o valor a ser adicionado ao saldo:")
  (let [valor (read-line)]
    (try
      (let [valor-num (Double/parseDouble valor)]
        (if (pos? valor-num)
          (do
            (saldo/atualizar-saldo valor-num)
            (println (str "Saldo atualizado com sucesso! Saldo atual: R$" (saldo/consultar-saldo))))
          (println "Valor invalido! Deve ser maior que zero.")))
      (catch Exception _ 
        (println "Erro: valor invalido!")))))

(defn consultar-saldo []
  (println (str "Seu saldo atual é: R$" (saldo/consultar-saldo))))

(defn escolher-esporte []
  (println "Buscando esportes disponiveis...")
  (let [sport-id (handlers/listar-esportes {})]
    (if sport-id
      (do
        (reset! selected-sport-id sport-id)
        (println (str "Esporte selecionado com sucesso: ID " sport-id)))
      (println "Falha ao selecionar um esporte. Tente novamente."))))

(defn validar-data [data]
  "Valida se a data está no formato YYYY-MM-DD."
  (boolean (re-matches #"\d{4}-\d{2}-\d{2}" data)))

(defn extrair-data [data-str]
  "Extrai a data de uma string no formato ISO8601."
  (first (str/split data-str #"T")))

(defn formatar-jogo [jogo]
  "Formata os detalhes de um jogo para exibição organizada."
  (str "Home Team: " (:home_team jogo) "\n"
       "Away Team: " (:away_team jogo) "\n"
       "Event Date: " (:date_event jogo) "\n"
       "Event Name: " (:event_name jogo) "\n"
       "Location: " (:event_location jogo) "\n"
       "League: " (:league_name jogo) "\n"
       "Broadcast: " (:broadcast jogo) "\n"
       "---"))

(defn filtrar-jogos-por-data []
  "Filtra jogos da API pela data inserida pelo usuário no formato YYYY-MM-DD."
  (if-let [sport-id @selected-sport-id]
    (do
      (println "Digite a data (formato YYYY-MM-DD) para filtrar os jogos:")
      (let [data (read-line)]
        (if (validar-data data)
          (try
            (let [response (handlers/listar-schedules {:path-params {:sport-id sport-id}})
                  schedules (-> response :body (json/parse-string true) :schedules)
                  jogos (filter #(= data (extrair-data (:date_event %))) schedules)]
              (if (seq jogos)
                (do
                  (reset! selected-date data)      
                  (reset! filtered-games jogos)    
                  (println "\n--- JOGOS ENCONTRADOS ---")
                  (doseq [[idx jogo] (map-indexed vector jogos)]
                    (println (str (inc idx) ". " (formatar-jogo jogo))))
                  jogos)
                (println "Nenhum jogo encontrado para esta data.")))
            (catch Exception e
              (println "Erro ao buscar jogos: " (.getMessage e))))
          (println "Data invalida! Certifique-se de usar o formato YYYY-MM-DD."))))
    (println "Por favor, escolha um esporte primeiro (opção 3).")))

(defn formatar-odds [odds]
  "Formata as odds para exibição."
  (let [home-odds (get odds :moneyline_home)
        away-odds (get odds :moneyline_away)
        draw-odds (get odds :moneyline_draw)]
    (str "Home Odds: " home-odds
         ", Away Odds: " away-odds
         (when draw-odds (str ", Draw Odds: " draw-odds)))))

(defn american-to-decimal [odds]
  "Converte odds no formato American para Decimal."
  (if (pos? odds)
    (format "%.2f" (+ (/ odds 100.0) 1))
    (format "%.2f" (+ (/ 100.0 (Math/abs odds)) 1))))

(defn formatar-jogo-com-odds [jogo odds handicap]
  (let [home-odds (if odds (american-to-decimal (:moneyline_home odds)) "N/D")
        draw-odds (if odds (american-to-decimal (:moneyline_draw odds)) "N/D")
        away-odds (if odds (american-to-decimal (:moneyline_away odds)) "N/D")
        home-handicap (if handicap (american-to-decimal (:point_spread_home_money handicap)) "N/D")
        away-handicap (if handicap (american-to-decimal (:point_spread_away_money handicap)) "N/D")]
    (str "Home Team: " (:home_team jogo) "\n"
         "Away Team: " (:away_team jogo) "\n"
         "Event Date: " (:date_event jogo) "\n"
         "Event Name: " (:event_name jogo) "\n"
         "Odds: Home " home-odds ", Draw " draw-odds ", Away " away-odds "\n"
         "Handicap: Home " home-handicap " (" (:point_spread_home handicap) "), "
         "Away " away-handicap " (" (:point_spread_away handicap) ")\n"
         "---")))

(defn realizar-aposta []
  (if-not @selected-sport-id
    (println "Por favor, escolha um esporte primeiro (opção 3).")
    (if (empty? @filtered-games)
      (println "Por favor, filtre os jogos por data primeiro (opção 4).")
      (do
        (println "\n--- JOGOS DISPONiVEIS ---")
        (doseq [[idx jogo] (map-indexed vector @filtered-games)]
          (let [event-id (:event_id jogo)
                odds-response (utils/fetch-odds event-id)]
            (if (and odds-response (:moneyline odds-response))
              (let [odds (:moneyline odds-response)
                    handicap (:spread odds-response)]
                (println (str (inc idx) ". " (formatar-jogo-com-odds jogo odds handicap))))
              (println (str (inc idx) ". " (formatar-jogo jogo) "\nOdds ou handicap nao disponiveis.\n---")))))

        (println "Escolha o numero do jogo para apostar:")
        (let [escolha (read-line)]
          (try
            (let [indice (Integer/parseInt escolha)
                  jogo (nth @filtered-games (dec indice))
                  event-id (:event_id jogo)
                  odds-response (utils/fetch-odds event-id)
                  odds (:moneyline odds-response)
                  handicap (:spread odds-response)]

              (println "Escolha o mercado para apostar:")
              (println "1. Vitória/Empate/Derrota")
              (println "2. Handicap")
              (let [mercado-escolha (read-line)]
                (case mercado-escolha
                  "1"
                  (do
                    (println "Escolha a opção:")
                    (println "1. Vitória Home")
                    (println "2. Empate")
                    (println "3. Vitória Away")
                    (let [opcao (read-line)
                          selecao (case opcao
                                    "1" "Home"
                                    "2" "Draw"
                                    "3" "Away"
                                    (throw (Exception. "Opção inválida!")))]
                      (println "Digite o valor da aposta:")
                      (let [valor (read-line)
                            valor-num (Double/parseDouble valor)]
                        (if (<= valor-num (saldo/consultar-saldo))
                          (if (saldo/realizar-aposta valor-num)
                            (do
                              (swap! apostas conj {:jogo jogo :mercado "Vitória/Empate/Derrota"
                                                   :selecao selecao :valor valor-num :status :pendente})
                              (println (str "Aposta realizada com sucesso no jogo: " (:event_name jogo))))
                            (println "Erro ao processar a aposta."))
                          (println "Erro: o valor da aposta excede o saldo disponível.")))))

                  "2"
                  (do
                    (println "Escolha a opção:")
                    (println (str "1. Handicap Home (" (:point_spread_home handicap) ")"))
                    (println (str "2. Handicap Away (" (:point_spread_away handicap) ")"))
                    (let [opcao (read-line)
                          selecao (case opcao
                                    "1" "Handicap Home"
                                    "2" "Handicap Away"
                                    (throw (Exception. "Opção invalida!")))]
                      (println "Digite o valor da aposta:")
                      (let [valor (read-line)
                            valor-num (Double/parseDouble valor)]
                        (if (<= valor-num (saldo/consultar-saldo))
                          (if (saldo/realizar-aposta valor-num)
                            (do
                              (swap! apostas conj {:jogo jogo :mercado "Handicap"
                                                   :selecao selecao :valor valor-num :status :pendente})
                              (println (str "Aposta realizada com sucesso no jogo: " (:event_name jogo))))
                            (println "Erro ao processar a aposta."))
                          (println "Erro: o valor da aposta excede o saldo disponível.")))))

                  (println "Mercado inválido!"))))
            (catch Exception e
              (println "Erro ao realizar aposta: " (.getMessage e)))))))))

(defn consultar-apostas-realizadas []
  (if (seq @apostas)
    (do
      (println "Apostas realizadas:")
      (pprint @apostas))
    (println "Nenhuma aposta realizada até o momento.")))

(defn liquidar-apostas []
  "Liquida as apostas pendentes e atualiza o saldo de acordo com o resultado do jogo."
  (println "---")
  (let [apostas-nao-liquidada (filter #(= (:status %) :pendente) @apostas)]
    (if (seq apostas-nao-liquidada)
      (doseq [aposta apostas-nao-liquidada]
        (let [event-id (:event_id (:jogo aposta))
              odds-response (utils/fetch-odds event-id)
              resultado (:resultado odds-response)
              selecao (:selecao aposta)
              odds (case selecao
                     "Home" (when odds-response
                              (american-to-decimal (get-in odds-response [:moneyline :moneyline_home])))
                     "Draw" (when odds-response
                              (american-to-decimal (get-in odds-response [:moneyline :moneyline_draw])))
                     "Away" (when odds-response
                              (american-to-decimal (get-in odds-response [:moneyline :moneyline_away])))
                     "Handicap Home" (when odds-response
                                       (american-to-decimal (get-in odds-response [:spread :point_spread_home_money])))
                     "Handicap Away" (when odds-response
                                       (american-to-decimal (get-in odds-response [:spread :point_spread_away_money]))))]
          (if (and odds resultado)
            (let [valor-aposta (:valor aposta)
                  lucro (- (* valor-aposta (Double/parseDouble odds)) valor-aposta)] 
              (cond
                (= selecao resultado)
                (do
                  (saldo/atualizar-saldo lucro)
                  (swap! apostas
                         (fn [apostas]
                           (map #(if (= % aposta) (assoc % :status :vencedora) %) apostas)))
                  (println (str "Aposta ganha! R$" (format "%.2f" lucro)
                                ". Saldo atualizado: R$" (saldo/consultar-saldo))))

                :else
                (do
                  (swap! apostas
                         (fn [apostas]
                           (map #(if (= % aposta) (assoc % :status :perdida) %) apostas)))
                  (println "Aposta perdida."))))
            (println (str "Dados insuficientes para processar a aposta no evento " event-id)))))
      (println "Nenhuma aposta pendente para liquidar.")))
  (println "---"))


(defn executar-menu []
  (loop []
    (mostrar-menu)
    (let [opcao (read-line)]
      (case opcao
        "1" (adicionar-saldo)
        "2" (consultar-saldo)
        "3" (escolher-esporte)
        "4" (filtrar-jogos-por-data)
        "5" (realizar-aposta)
        "6" (consultar-apostas-realizadas)
        "7" (liquidar-apostas)
        "8" (do (println "Saindo...") (System/exit 0))
        (println "Opcao inválida!"))
      (recur))))

(defn -main
  "Inicia o menu interativo e o servidor simultaneamente."
  [& args]
  (println "Iniciando o servidor na porta 8080...")
  (future (service/start))
  (println "Iniciando menu interativo...")
  (executar-menu))