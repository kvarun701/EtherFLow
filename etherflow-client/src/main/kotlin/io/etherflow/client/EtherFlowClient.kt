package io.etherflow.client

import io.etherflow.core.Mono

/**
 * Kotlin reified extension: deserializes response body to [T] without passing Class token.
 *
 * ```kotlin
 * val user: Mono<User> = client.get()
 *     .uri("/users/{id}", id)
 *     .retrieve()
 *     .bodyTo<User>()
 * ```
 */
inline fun <reified T> ResponseSpec.bodyTo(): Mono<T> = bodyTo(T::class.java)

/**
 * Kotlin reified extension for parameterized types (e.g., List<User>).
 *
 * ```kotlin
 * val users: Mono<List<User>> = client.get()
 *     .uri("/users")
 *     .retrieve()
 *     .bodyTo<List<User>>()
 * ```
 */
inline fun <reified T> ResponseSpec.bodyToRef(): Mono<T> = bodyTo(object : ParameterizedTypeReference<T>() {})

/**
 * Kotlin reified extension: deserializes response body to [T] as a [Result].
 */
inline fun <reified T> ResponseSpec.toResult(): Mono<Result<T>> = toResult(T::class.java)
