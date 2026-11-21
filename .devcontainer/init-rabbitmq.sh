#!/bin/bash
# Sobe o RabbitMQ e cria o usuário da aplicação (lab / lab123). Pode rodar várias vezes.
set -e

# PIDs de uma sessão anterior do Codespace não valem mais
rm -rf run

if ! rabbitmqctl -q status >/dev/null 2>&1; then
    echo "Iniciando RabbitMQ..."
    rabbitmq-server -detached
fi

echo "Aguardando RabbitMQ ficar pronto..."
for tentativa in $(seq 1 60); do
    if rabbitmqctl -q status >/dev/null 2>&1; then
        break
    fi
    sleep 1
done
rabbitmqctl await_startup

if ! rabbitmqctl -q list_users | grep -q "^lab"; then
    rabbitmqctl add_user lab lab123
fi
rabbitmqctl set_user_tags lab administrator
rabbitmqctl set_permissions -p / lab ".*" ".*" ".*"

echo "RabbitMQ pronto: amqp://localhost:5672 | painel http://localhost:15672 (lab / lab123)"
