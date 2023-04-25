package org.stellar.anchor.api.webhook.fireblocks;

public enum TransactionStatus {
  SUBMITTED,
  QUEUED,
  PENDING_AUTHORIZATION,
  PENDING_SIGNATURE,
  BROADCASTING,
  PENDING_3RD_PARTY_MANUAL_APPROVAL,
  PENDING_3RD_PARTY,
  CONFIRMING,
  PARTIALLY_COMPLETED,
  PENDING_AML_SCREENING,
  COMPLETED,
  CANCELLED,
  REJECTED,
  BLOCKED,
  FAILED
}
