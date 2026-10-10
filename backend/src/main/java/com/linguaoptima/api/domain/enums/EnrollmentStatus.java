/**
 * @file EnrollmentStatus.java
 * @brief Lifecycle statuses for student cohort group membership invitations.
 */
package com.linguaoptima.api.domain.enums;

/**
 * @brief Lifecycle statuses for student cohort group membership invitations.
 */
public enum EnrollmentStatus {
    /** @brief Invitation sent by teacher, pending student acceptance or decline. */
    PENDING,
    /** @brief Student accepted group membership invitation. */
    ACCEPTED,
    /** @brief Student declined group membership invitation. */
    DECLINED
}
