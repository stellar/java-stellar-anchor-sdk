package org.stellar.reference.callbacks.customer

import java.util.*
import org.stellar.anchor.api.callback.GetCustomerRequest
import org.stellar.anchor.api.callback.GetCustomerResponse
import org.stellar.anchor.api.callback.PutCustomerRequest
import org.stellar.anchor.api.callback.PutCustomerResponse
import org.stellar.anchor.api.shared.CustomerField
import org.stellar.anchor.api.shared.ProvidedCustomerField
import org.stellar.reference.callbacks.BadRequestException
import org.stellar.reference.callbacks.NotFoundException
import org.stellar.reference.dao.CustomerRepository
import org.stellar.reference.model.Customer
import org.stellar.reference.model.Status

class CustomerService(private val customerRepository: CustomerRepository) {
  fun getCustomer(request: GetCustomerRequest): GetCustomerResponse {
    val customer =
      when {
        request.id != null -> {
          customerRepository.get(request.id)
            ?: throw NotFoundException("customer for 'id' '${request.id}' not found", request.id)
        }
        request.account != null -> {
          customerRepository.get(request.account, request.memo, request.memoType)
            ?: Customer(
              stellarAccount = request.account,
              memo = request.memo,
              memoType = request.memoType
            )
        }
        else -> {
          throw BadRequestException("Either id or account must be provided")
        }
      }
    return convertCustomerToResponse(customer, request.type)
  }

  fun upsertCustomer(request: PutCustomerRequest): PutCustomerResponse {
    val customer =
      when {
        request.id != null -> customerRepository.get(request.id)
        request.account != null ->
          customerRepository.get(request.account, request.memo, request.memoType)
        else -> {
          throw BadRequestException("Either id or account must be provided")
        }
      }

    // Update the customer if it exists, otherwise create a new one.
    if (customer != null) {
      customerRepository.update(
        customer.copy(
          firstName = request.firstName ?: customer.firstName,
          lastName = request.lastName ?: customer.lastName,
          emailAddress = request.emailAddress ?: customer.emailAddress,
          bankAccountNumber = request.bankAccountNumber ?: customer.bankAccountNumber,
          bankAccountType = request.bankAccountType ?: customer.bankAccountType,
          bankRoutingNumber = request.bankNumber ?: customer.bankRoutingNumber,
          clabeNumber = request.clabeNumber ?: customer.clabeNumber
        )
      )
      return PutCustomerResponse(customer.id)
    } else {
      val id = UUID.randomUUID().toString()
      customerRepository.create(
        Customer(
          id = id,
          stellarAccount = request.account,
          memo = request.memo,
          memoType = request.memoType,
          firstName = request.firstName,
          lastName = request.lastName,
          emailAddress = request.emailAddress,
          bankAccountNumber = request.bankAccountNumber,
          bankAccountType = request.bankAccountType,
          bankRoutingNumber = request.bankNumber,
          clabeNumber = request.clabeNumber
        )
      )
      return PutCustomerResponse(id)
    }
  }

  fun deleteCustomer(id: String) {
    customerRepository.delete(id)
  }

  fun invalidateClabe(id: String) {
    try {
      customerRepository.update(customerRepository.get(id)!!.copy(clabeNumber = null))
    } catch (e: Exception) {
      throw NotFoundException("customer for 'id' '$id' not found", id)
    }
  }

  private fun convertCustomerToResponse(customer: Customer, type: String?): GetCustomerResponse {
    val providedFields = mutableMapOf<String, ProvidedCustomerField>()
    val missingFields = mutableMapOf<String, CustomerField>()

    val commonFields =
      mapOf(
        "first_name" to createField(customer.firstName, "string", "The customer's first name"),
        "last_name" to createField(customer.lastName, "string", "The customer's last name"),
        "email_address" to
          createField(customer.emailAddress, "string", "The customer's email address"),
      )
    val sep31ReceiverFields =
      mapOf(
        "bank_account_number" to
          createField(customer.bankAccountNumber, "string", "The customer's bank account number"),
        "bank_account_type" to
          createField(
            customer.bankAccountType,
            "string",
            "The customer's bank account type",
            choices = listOf("checking", "savings")
          ),
        "bank_number" to
          createField(customer.bankRoutingNumber, "string", "The customer's bank routing number"),
        "clabe_number" to createField(customer.clabeNumber, "string", "The customer's CLABE number")
      )
    val fields =
      when (type) {
        "sep31-receiver" -> commonFields.plus(sep31ReceiverFields)
        else -> commonFields
      }

    // Extract fields from customer
    fields.forEach(
      fun(entry: Map.Entry<String, Field>) {
        when (entry.value) {
          is Field.Provided -> providedFields[entry.key] = (entry.value as Field.Provided).field
          is Field.Missing -> missingFields[entry.key] = (entry.value as Field.Missing).field
        }
      }
    )

    val status =
      when {
        missingFields.isNotEmpty() -> Status.NEEDS_INFO
        else -> Status.ACCEPTED
      }.toString()

    return GetCustomerResponse.builder()
      .id(customer.id)
      .status(status)
      .providedFields(providedFields)
      .fields(missingFields)
      .build()
  }

  sealed class Field {
    class Provided(val field: ProvidedCustomerField) : Field()
    class Missing(val field: CustomerField) : Field()
  }
  private fun createField(
    value: Any?,
    type: String,
    description: String,
    optional: Boolean? = false,
    choices: List<String>? = listOf()
  ): Field {
    return when (value != null) {
      true -> {
        var builder =
          ProvidedCustomerField.builder()
            .type(type)
            .description(description)
            .status(Status.ACCEPTED.toString())
            .optional(optional)
        if (choices != null) {
          builder = builder.choices(choices)
        }
        Field.Provided(builder.build())
      }
      false -> {
        var builder = CustomerField.builder().type(type).description(description).optional(optional)
        if (choices != null) {
          builder = builder.choices(choices)
        }
        Field.Missing(builder.build())
      }
    }
  }
}
