cd data-sand
mvn clean install -DskipTests=true
pause
cd ..
jar -cvf ds.jar ./data-sand/*
cp ds.jar /home/saichler/.
chmod 777 /home/saichler/ds.jar
