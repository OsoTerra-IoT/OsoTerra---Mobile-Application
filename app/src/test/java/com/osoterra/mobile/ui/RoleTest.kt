package com.osoterra.mobile.ui

import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.model.UserRole
import com.osoterra.mobile.ui.common.canWrite
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleTest {

    private fun user(role: UserRole) = User(id = "1", fullName = "Ana", email = "ana@example.com", role = role)

    @Test
    fun `the producer can write`() = assertTrue(user(UserRole.PRODUCER).canWrite())

    @Test
    fun `the advisor only reads`() = assertFalse(user(UserRole.ADVISOR).canWrite())

    @Test
    fun `nothing is offered until the user is known`() = assertFalse((null as User?).canWrite())
}
