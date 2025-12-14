FROM clojure

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY . .

#RUN mkdir .db/

EXPOSE 3000

RUN clojure -T:build uber

CMD ["./start.sh"]
