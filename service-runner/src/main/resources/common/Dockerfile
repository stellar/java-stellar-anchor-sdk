FROM node:18

RUN mkdir -p /app
WORKDIR /app

RUN git clone https://github.com/stellar/sep24-reference-ui.git
WORKDIR /app/sep24-reference-ui
RUN yarn install
RUN yarn build