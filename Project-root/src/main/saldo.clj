(ns main.saldo
  (:require [cheshire.core :as json]))

(def saldo (atom {:valor 0.0}))

(defn consultar-saldo []
  "Retorna o saldo atual."
  (:valor @saldo))

(defn atualizar-saldo [valor]
  "Atualiza o saldo do usuário (adiciona ou subtrai)."
  (if (>= (+ (:valor @saldo) valor) 0)
    (swap! saldo update :valor + valor)
    {:erro "Saldo insuficiente para esta operação"}))

(defn realizar-aposta [valor]
  "Realiza uma aposta, debitando o valor do saldo."
  (if (>= (:valor @saldo) valor)
    (do
      (swap! saldo update :valor - valor)
      {:mensagem (str "Aposta de R$" valor " realizada com sucesso! Saldo atualizado: R$" (:valor @saldo))})
    {:erro "Saldo insuficiente para realizar a aposta"}))

(defn liquidar-aposta [valor ganhou?]
  "Liquida uma aposta. Se o usuário ganhar, o saldo é atualizado com o valor ganho."
  (if ganhou?
    (swap! saldo update :valor + valor)
    (str "Aposta perdida. Saldo atual: R$" (:valor @saldo))))

(defn resetar-saldo []
  "Reseta o saldo para 0."
  (reset! saldo {:valor 0.0}))
