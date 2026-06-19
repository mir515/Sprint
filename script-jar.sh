#!/bin/bash
# 1. Aller dans le dossier du framework
cd /home/princy/Documents/NAINA/framework
mkdir -p bin



echo "Compilation des sources..."
find src -name "*.java" > sources.txt
javac -cp "lib/servlet-api.jar" -d bin @sources.txt
rm sources.txt

echo "Création du framework.jar..."
cd bin
jar cvf ../framework.jar .
cd ..

echo "Terminé ! Votre fichier framework.jar est prêt."
