rm target/* -r
mvn compile assembly:single
cp target/DBApi-1.0-jar-with-dependencies.jar .
mkdir server -p
rm server/* -rf
cp DBApi-1.0-jar-with-dependencies.jar server/.
cp -r public_html server/.
cp -r cfg server/.
cp -r start.sh server/.
tar czf server.tar.gz server
mv server.tar.gz server/.
