package org.stellar.anchor.api.sep.sep24

import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.AssetInfo

internal class Sep24DtoTests {
  @Test
  fun `test to cover AssetInfo`() {
    val ar = AssetInfo()
    ar.getSignificantDecimals()
    ar.setSignificantDecimals(0)
    ar.getSend()
    ar.setSend(AssetInfo.SendOperation())
    ar.getSep31Enabled()
    ar.setSep31Enabled(true)

    val ao = AssetInfo.AssetOperation()
    ao.getFeeFixed()
    ao.setFeeFixed(0)
    ao.getFeePercent()
    ao.setFeePercent(0)
    ao.getFeeMinimum()
    ao.setFeeMinimum(0)

    val so = AssetInfo.SendOperation()
    so.getFeeFixed()
    so.setFeeFixed(0)
    so.getFeePercent()
    so.setFeePercent(0)
    so.getMinAmount()
    so.setMinAmount(0)
    so.getMaxAmount()
    so.setMaxAmount(0)
  }

  @Test
  fun `test to cover WithdrawDepositTransactionResponse`() {
    val dtr = DepositTransactionResponse()
    dtr.setDepositMemo("")
    dtr.getDepositMemo()
    dtr.setDepositMemoType("id")
    dtr.getDepositMemoType()
    dtr.setClaimableBalanceId("0")
    dtr.getClaimableBalanceId()
    dtr.canEqual(Object())
    dtr.hashCode()

    val wtr = WithdrawTransactionResponse()
    wtr.getWithdrawMemo()
    wtr.setWithdrawMemo("")
    wtr.getWithdrawMemoType()
    wtr.setWithdrawMemoType("")
    wtr.getWithdrawAnchorAccount()
    wtr.setWithdrawAnchorAccount("")
    wtr.canEqual(Object())
    wtr.canEqual(Object())
    wtr.hashCode()
  }

  @Test
  fun `test to cover GetTransactionRequest`() {
    val gtr = GetTransactionRequest("", "", "", "")
    gtr.canEqual(Object())
  }

  @Test
  fun `test to cover InfoResponse`() {
    val ir = InfoResponse()
    ir.getFeatureFlags()
    ir.setFeatureFlags(InfoResponse.FeatureFlagResponse())

    val fr = InfoResponse.FeeResponse()
    fr.getEnabled()
    fr.canEqual(Object())

    val ffr = InfoResponse.FeatureFlagResponse()
    ffr.getAccountCreation()
    ffr.setAccountCreation(true)
    ffr.getClaimableBalances()
    ffr.setClaimableBalances(true)
  }

  @Test
  fun `test to cover InteractiveTransactionResponse`() {
    val itr = InteractiveTransactionResponse("", "", "")
    itr.getType()
    itr.setType("")
    itr.getId()
    itr.setId("")
    itr.getUrl()
    itr.setUrl("")
    itr.canEqual(Object())
  }

  @Test
  fun `test to cover TransactionResponse`() {
    val tr = TransactionResponse()
    tr.getStatus_eta()
    tr.setStatus_eta(1)
    tr.getMoreInfoUrl()
    tr.setMoreInfoUrl("")
    tr.getAmountIn()
    tr.setAmountIn("1")
    tr.getAmountInAsset()
    tr.setAmountInAsset("")
    tr.getAmountOut()
    tr.setAmountOut("1")
    tr.getAmountOutAsset()
    tr.setAmountOutAsset("")
    tr.getAmountFee()
    tr.setAmountFee("1")
    tr.getAmountFeeAsset()
    tr.setAmountFeeAsset("1")
    tr.getStellarTransactionId()
    tr.setStellarTransactionId("")
    tr.getExternalTransactionId()
    tr.setExternalTransactionId("")
    tr.getMessage()
    tr.setMessage("")
    tr.getAccountMemo()
    tr.setAccountMemo("")
    tr.getMuxedAccount()
    tr.setMuxedAccount("")
  }
}
