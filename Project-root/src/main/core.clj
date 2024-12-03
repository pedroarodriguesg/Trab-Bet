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
  (println "7. Sair")
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
          (println "Valor inválido! Deve ser maior que zero.")))
      (catch Exception _ 
        (println "Erro: valor inválido!")))))

(defn consultar-saldo []
  (println (str "Seu saldo atual é: R$" (saldo/consultar-saldo))))

(defn escolher-esporte []
  (println "Buscando esportes disponíveis...")
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
                  (reset! selected-date data)      ;; Salvar a data selecionada
                  (reset! filtered-games jogos)    ;; Salvar os jogos filtrados
                  (println "\n--- JOGOS ENCONTRADOS ---")
                  (doseq [[idx jogo] (map-indexed vector jogos)]
                    (println (str (inc idx) ". " (formatar-jogo jogo))))
                  jogos)
                (println "Nenhum jogo encontrado para esta data.")))
            (catch Exception e
              (println "Erro ao buscar jogos: " (.getMessage e))))
          (println "Data inválida! Certifique-se de usar o formato YYYY-MM-DD."))))
    (println "Por favor, escolha um esporte primeiro (opção 3).")))

(defn formatar-odds [odds]
  "Formata as odds para exibição."
  (let [home-odds (get odds :moneyline_home)
        away-odds (get odds :moneyline_away)
        draw-odds (get odds :moneyline_draw)]
    (str "Home Odds: " home-odds
         ", Away Odds: " away-odds
         (when draw-odds (str ", Draw Odds: " draw-odds)))))

(defn formatar-jogo-com-odds [jogo odds]
  "Formata os detalhes de um jogo incluindo as odds para exibição organizada."
  (str "Home Team: " (:home_team jogo) "\n"
       "Away Team: " (:away_team jogo) "\n"
       "Event Date: " (:date_event jogo) "\n"
       "Event Name: " (:event_name jogo) "\n"
       (formatar-odds odds) "\n"
       "---"))

(defn realizar-aposta []
  (if-let [sport-id @selected-sport-id]
    (if (seq @filtered-games)
      (do
        (let [jogos @filtered-games]
          ;; Exibir jogos com odds atualizadas
          (println "\n--- JOGOS DISPONÍVEIS PARA APOSTA ---")
          (doseq [[idx jogo] (map-indexed vector jogos)]
            (let [game-id (:event_id jogo)
                  odds (utils/fetch-odds game-id)]
              (if odds
                (println (str (inc idx) ". " (formatar-jogo-com-odds jogo odds)))
                (println (str (inc idx) ". " (formatar-jogo jogo) "\nOdds nao disponíveis.\n---")))))
          ;; Solicitar que o usuário escolha um jogo
          (println "Escolha o número do jogo:")
          (let [escolha (read-line)]
            (try
              (let [indice (Integer/parseInt escolha)
                    jogo (nth jogos (dec indice))]
                ;; Prosseguir com a aposta
                (println "Digite o valor da aposta:")
                (let [valor (read-line)
                      valor-num (Double/parseDouble valor)]
                  (if (saldo/realizar-aposta valor-num)
                    (do
                      (swap! apostas conj {:jogo jogo :valor valor-num})
                      (println (str "Aposta realizada com sucesso no jogo: " (:event_name jogo)))
                      (println (str "Saldo atualizado: R$" (saldo/consultar-saldo))))
                    (println "Erro: saldo insuficiente."))))
              (catch Exception e
                (println "Erro: opção ou valor inválido!" (.getMessage e))))))
      (println "Por favor, filtre os jogos por data primeiro (opção 4).")))
    (println "Por favor, escolha um esporte primeiro (opção 3).")))


(defn consultar-apostas-realizadas []
  (if (seq @apostas)
    (do
      (println "Apostas realizadas:")
      (pprint @apostas))
    (println "Nenhuma aposta realizada até o momento.")))

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
        "7" (do (println "Saindo...") (System/exit 0))
        (println "Opcao inválida!"))
      (recur))))

(defn -main
  "Inicia o menu interativo e o servidor simultaneamente."
  [& args]
  (println "Iniciando o servidor na porta 8080...")
  (future (service/start))
  (println "Iniciando menu interativo...")
  (executar-menu))