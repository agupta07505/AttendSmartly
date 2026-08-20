/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly

import com.agupta07505.attendsmartly.util.GitHubUpdateChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateCheckerTest {

    @Test
    fun testVersionComparison() {
        // Newer versions
        assertTrue(GitHubUpdateChecker.isVersionNewer(remote = "1.3", current = "1.2"))
        assertTrue(GitHubUpdateChecker.isVersionNewer(remote = "2.0.0", current = "1.9.9"))
        assertTrue(GitHubUpdateChecker.isVersionNewer(remote = "1.2.1", current = "1.2"))
        assertTrue(GitHubUpdateChecker.isVersionNewer(remote = "1.10", current = "1.2"))

        // Same or older versions
        assertFalse(GitHubUpdateChecker.isVersionNewer(remote = "1.2", current = "1.2"))
        assertFalse(GitHubUpdateChecker.isVersionNewer(remote = "1.1", current = "1.2"))
        assertFalse(GitHubUpdateChecker.isVersionNewer(remote = "1.2.0", current = "1.2"))
        assertFalse(GitHubUpdateChecker.isVersionNewer(remote = "", current = "1.2"))
    }
}
