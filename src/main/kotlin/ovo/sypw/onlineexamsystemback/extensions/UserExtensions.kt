package ovo.sypw.onlineexamsystemback.extensions

import ovo.sypw.onlineexamsystemback.entity.User

/**
 * Safely retrieve the non-null ID of a User entity.
 * Throws IllegalStateException if the entity has not been persisted yet.
 */
val User.safeId: Long
    get() = id ?: throw IllegalStateException("User entity has not been persisted yet")
