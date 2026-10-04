#!/bin/bash
cd /home/princy/Documents/NAINA/Sprint || exit
mkdir -p bin

echo "Compilation des sources pour Java 21..."
find src -name "*.java" > sources.txt

javac --release 21 -cp "lib/servlet-api.jar:lib/gson.jar" -d bin @sources.txt
rm sources.txt

echo "Création du framework.jar..."
cd bin
jar cvf ../framework.jar .
cd ..

echo "Terminé ! Votre fichier framework.jar est prêt."