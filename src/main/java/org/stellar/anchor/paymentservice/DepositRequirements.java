package org.stellar.anchor.paymentservice;

import lombok.Data;
import reactor.util.annotation.Nullable;

/**
 * Contains the configuration options needed for an external user to make a deposit into the desired account. This class
 * will be needed if the implementation allows users to make deposits using intermediary networks. For instance, when a
 * user wants to make a deposit to their Circle account through a Stellar payment.
 *
 * @see DepositInstructions
 * @see PaymentsService#getDepositInstructions(DepositRequirements)
 */
@Data
@SuppressWarnings("unused")
public class DepositRequirements {
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
     * The network where the deposit will happen. After the deposit is performed on that network it will be reflected in
     * the beneficiary user balance. Time for confirmation and reconciliation may be needed depending on the network
     * used.
     */
    Network intermediaryNetwork;

    /**
     * The name of the currency that will be ultimately credited into the beneficiary user account. It should obey the
     * {scheme}:{identifier} format described in <a href="https://stellar.org/protocol/sep-38#asset-identification-format">SEP-38</a>.
     */
    String currencyName;

    /**
     * Constructor of the DepositConfiguration class.
     *
     * @param beneficiaryAccountId Identifier of the account who will receive the payment.
     * @param intermediaryNetwork  The network where the deposit will be made. After the deposit is performed on that network it will be reflected in the beneficiary user balance
     * @param currencyName         The name of the currency that will be ultimately credited into the beneficiary user account.
     */
    public DepositRequirements(String beneficiaryAccountId, Network intermediaryNetwork, String currencyName) {
        this(beneficiaryAccountId, null, intermediaryNetwork, currencyName);
    }

    /**
     * Constructor of the DepositConfiguration class.
     *
     * @param beneficiaryAccountId    Identifier of the account who will receive the payment.
     * @param beneficiaryAccountIdTag Complementary identifier of the account who will receive the payment. May be mandatory depending on the implementation.
     * @param intermediaryNetwork     The network where the deposit will be made. After the deposit is performed on that network it will be reflected in the beneficiary user balance
     * @param currencyName            The name of the currency that will be ultimately credited into the beneficiary user account.
     */
    public DepositRequirements(String beneficiaryAccountId, @Nullable String beneficiaryAccountIdTag, Network intermediaryNetwork, String currencyName) {
        this.beneficiaryAccountId = beneficiaryAccountId;
        this.beneficiaryAccountIdTag = beneficiaryAccountIdTag;
        this.intermediaryNetwork = intermediaryNetwork;
        this.currencyName = currencyName;
    }
}
