# Java 21 + Maven 3.9 (Ubuntu 24.04)
FROM maven:3.9-eclipse-temurin-21-noble

ENV DEBIAN_FRONTEND=noninteractive

# rabbitmq-server -> broker de mensagens usado no trabalho
# curl / jq       -> disparar eventos e ler as respostas JSON nos scripts
# (policy-rc.d evita que a instalação tente iniciar o serviço durante o build)
RUN printf '#!/bin/sh\nexit 101\n' > /usr/sbin/policy-rc.d && chmod +x /usr/sbin/policy-rc.d \
    && apt-get update && apt-get install -y \
    rabbitmq-server \
    git \
    curl \
    jq \
    unzip \
    zip \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /usr/sbin/policy-rc.d

# Nome fixo do nó (o hostname do Codespace muda) e painel web de administração (porta 15672)
RUN echo "NODENAME=rabbit@localhost" > /etc/rabbitmq/rabbitmq-env.conf \
    && echo "[rabbitmq_management]." > /etc/rabbitmq/enabled_plugins
