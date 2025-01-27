#!/bin/bash

# If docker compose is up, stop it
if [ "$(docker-compose ps -q)" ]; then
  docker-compose down
# If docker compose is stopped, start it
elif [ ! "$(docker-compose ps -q)" ]; then
  docker-compose up -d
fi