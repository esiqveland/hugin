git pull
#cd src/main/frontend || exit
#npm run build
#cd ../../../
./gradlew clean jar
java --enable-preview -jar build/libs/hugin-1.0.jar
