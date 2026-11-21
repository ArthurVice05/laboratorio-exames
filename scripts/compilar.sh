#!/bin/bash
# Gera target/laboratorio.jar (um jar para o produtor e os cinco consumidores)
source "$(dirname "$0")/comum.sh"
cd "$RAIZ" && ./mvnw -q -DskipTests package && echo "Gerado: target/laboratorio.jar"
