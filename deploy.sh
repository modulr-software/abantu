cd /home/merv/Developer/abantu-be-staging

sudo systemctl stop abantu-api

git pull

./build.sh

sudo systemctl start abantu-api

