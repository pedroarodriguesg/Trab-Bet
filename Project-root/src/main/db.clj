(ns main.db
  (:require [monger.core :as mg]
            [monger.collection :as mc]))

(def conn (atom nil)) 
(def db (atom nil))   
(def apostas (atom [])) 

(defn connect!
  "Conecta ao MongoDB e inicializa os átomos de conexão."
  []
  (try
    (let [connection (mg/connect)
          database (mg/get-db connection "nome-do-banco")]
      (reset! conn connection)
      (reset! db database)
      (println "Conectado ao MongoDB"))
    (catch Exception e
      (println "Erro ao conectar ao MongoDB:" (.getMessage e)))))

(defn salvar-aposta-no-banco
  "Salva uma aposta no banco de dados MongoDB."
  [aposta]
  (when @db
    (mc/insert @db "apostas" aposta)))

(defn salvar-aposta
  "Salva uma aposta em um átomo e no banco de dados."
  [aposta]
  (swap! apostas conj aposta)
  (try
    (salvar-aposta-no-banco aposta)
    (catch Exception e
      (println "Erro ao salvar no banco de dados:" (.getMessage e)))))
