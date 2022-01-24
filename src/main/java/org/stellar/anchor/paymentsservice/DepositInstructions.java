package org.stellar.anchor.paymentsservice;

import lombok.Data;
import reactor.util.annotation.Nullable;

/**
 * The instructions needed to make a deposit into a beneficiary account using an intermediary account.
 *
 * @see DepositRequirements
 * @see PaymentsService#getDepositInstructions(DepositRequirements)
 */
@Data
@SuppressWarnings("unused")
public class DepositInstructions {
    /**
     * The internal identifier of the beneficiary account, i.e. the account who will receive the payment.
     */
    String beneficiaryAccountId;

    /**
     * A complementary identifier of the beneficiary account who will receive the payment. It might be considered
     * mandatory depending on the use case.
     */
    @Nullable
    String beneficiaryAccountIdTag;

    /**
     * The network of the deposit beneficiary. It is the network where the deposit will be ultimately credited.
     */
    Network beneficiaryNetwork;

    /**
     * The identifier of the intermediary account who will receive the deposit.
     */
    String intermediaryAccountId;

    /**
     * A complementary identifier of the intermediary account who will receive the deposit. It might be considered
     * mandatory depending on the use case.
     */
    @Nullable
    String intermediaryAccountIdTag;

    /**
     * The network where the deposit will be made. Some time after the deposit is performed on that network it will be
     * reflected in the beneficiary user balance. Time for confirmation and reconciliation may vary depending on the
     * network used.
     */
    Network intermediaryNetwork;

    /**
     * The name of the currency to be deposited into the intermediary network. It should obey the {scheme}:{identifier}
     * format described in <a href="https://stellar.org/protocol/sep-38#asset-identification-format">SEP-38</a>.
     */
    String currencyName;

    /**
     * Extra information needed to perform the deposit.
     */
    @Nullable
    Object extra;

    /**
     * Constructor for the DepositInstructions class
     *
     * @param beneficiaryAccountId     Identifier of the account who will receive the payment.
     * @param beneficiaryAccountIdTag  Complementary identifier of the account who will receive the payment. May be mandatory depending on the implementation.
     * @param beneficiaryNetwork       A complementary identifier of the intermediary account who will receive the deposit. It might be considered mandatory depending on the use case.
     * @param intermediaryAccountId    The identifier of the intermediary account who will receive the deposit.
     * @param intermediaryAccountIdTag A complementary identifier of the beneficiary account who will receive the payment.
     * @param intermediaryNetwork      The network where the deposit will be made. After the deposit is performed on that network it will be reflected in the beneficiary user balance
     * @param currencyName             The name of the currency to be deposited into the intermediary network.
     * @param extra                    Extra information needed to perform the deposit.
     */
    public DepositInstructions(String beneficiaryAccountId, @Nullable String beneficiaryAccountIdTag, Network beneficiaryNetwork, String intermediaryAccountId, @Nullable String intermediaryAccountIdTag, Network intermediaryNetwork, String currencyName, @Nullable Object extra) {
        this.beneficiaryAccountId = beneficiaryAccountId;
        this.beneficiaryAccountIdTag = beneficiaryAccountIdTag;
        this.beneficiaryNetwork = beneficiaryNetwork;
        this.intermediaryAccountId = intermediaryAccountId;
        this.intermediaryAccountIdTag = intermediaryAccountIdTag;
        this.intermediaryNetwork = intermediaryNetwork;
        this.currencyName = currencyName;
        this.extra = extra;
    }

    /**
     * Constructor for the DepositInstructions class
     *
     * @param beneficiaryAccountId  Identifier of the account who will receive the payment.
     * @param beneficiaryNetwork    A complementary identifier of the intermediary account who will receive the deposit. It might be considered mandatory depending on the use case.
     * @param intermediaryAccountId The identifier of the intermediary account who will receive the deposit.
     * @param intermediaryNetwork   The network where the deposit will be made. After the deposit is performed on that network it will be reflected in the beneficiary user balance
     * @param currencyName          The name of the currency to be deposited into the intermediary network.
     */
    public DepositInstructions(String beneficiaryAccountId, Network beneficiaryNetwork, String intermediaryAccountId, Network intermediaryNetwork, String currencyName) {
        this(beneficiaryAccountId, null, beneficiaryNetwork, intermediaryAccountId, null, intermediaryNetwork, currencyName, null);
    }
}
