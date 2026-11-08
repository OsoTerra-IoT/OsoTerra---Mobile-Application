package com.osoterra.mobile.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.osoterra.mobile.di.rememberAppContainer
import com.osoterra.mobile.domain.model.User
import com.osoterra.mobile.domain.model.UserRole

/**
 * Solo el productor cambia datos (parcelas, fincas, dispositivos, alertas): el asesor únicamente
 * consulta, igual que en el Backend, que responde 403 a sus escrituras. Mientras no se sabe
 * quién es, no se ofrece ninguna acción.
 */
fun User?.canWrite(): Boolean = this?.role == UserRole.PRODUCER

/** true cuando la persona que inició sesión puede crear o modificar datos. */
@Composable
fun rememberCanWrite(): Boolean {
    val container = rememberAppContainer()
    val user by produceState<User?>(initialValue = null) { value = container.authRepository.currentUser() }
    return user.canWrite()
}
