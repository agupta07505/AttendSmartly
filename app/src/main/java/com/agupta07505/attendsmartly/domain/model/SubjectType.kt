/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.domain.model

enum class SubjectType(val displayName: String) {
    LECTURE("Lecture"),
    TUTORIAL("Tutorial"),
    PRACTICAL_LAB("Practical Lab"),
    WORKSHOP("Workshop"),
    SEMINAR("Seminar"),
    OTHER("Other")
}
