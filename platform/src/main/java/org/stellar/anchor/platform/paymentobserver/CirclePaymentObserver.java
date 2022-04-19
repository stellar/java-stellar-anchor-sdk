package org.stellar.anchor.platform.paymentobserver;

public class CirclePaymentObserver {
}

// INCOMING:
//{
//  "clientId": "c60d2d5b-203c-45bb-9f6e-93641d40a599",
//  "notificationType": "transfers",
//  "transfer": {
//    "id": "7f131f58-a8a0-3dc2-be05-6a015c69de35",
//    "source": {
//      "type": "blockchain",
//      "chain": "XLM"
//    },
//    "destination": {
//      "type": "wallet",
//      "id": "1000223064",
//      "address": "GAYF33NNNMI2Z6VNRFXQ64D4E4SF77PM46NW3ZUZEEU5X7FCHAZCMHKU",
//      "addressTag": "8006436449031889621"
//    },
//    "amount": {
//      "amount": "1.50",
//      "currency": "USD"
//    },
//    "transactionHash": "fb8947c67856d8eb444211c1927d92bcf14abcfb34cdd27fc9e604b15d208fd1",
//    "status": "complete",
//    "createDate": "2022-02-07T18:02:17.999Z"
//  }
//}

// OUTGOING:
//{
//  "clientId": "c60d2d5b-203c-45bb-9f6e-93641d40a599",
//  "notificationType": "transfers",
//  "transfer": {
//    "id": "40c21a0d-a21c-44b1-9d49-2a7ce027e516",
//    "source": {
//      "type": "wallet",
//      "id": "1000662797"
//    },
//    "destination": {
//      "type": "blockchain",
//      "address": "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
//      "chain": "XLM"
//    },
//    "amount": {
//      "amount": "100.00",
//      "currency": "USD"
//    },
//    "transactionHash": "57c21fd8bbf900280299df455ced5a225dc1322baee1cbaa3f3b6751c9098a01",
//    "status": "complete",
//    "createDate": "2022-02-25T13:47:06.164Z"
//  }
//}