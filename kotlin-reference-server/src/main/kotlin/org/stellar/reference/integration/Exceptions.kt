package org.stellar.reference.integration

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class BadRequestException(val error: String, @Transient override val cause: Throwable? = null) :
  RuntimeException(error, cause)

@Serializable
class NotFoundException(
  val error: String,
  val id: String,
  @Transient override val cause: Throwable? = null
) : RuntimeException(error, cause)

@Serializable
class UnprocessableEntityException(
  val error: String,
  val id: String,
  @Transient override val cause: Throwable? = null
) : RuntimeException(error, cause)
