package afsm.sample.shop.core.data

import afsm.sample.shop.core.database.UserDao
import afsm.sample.shop.core.database.UserEntity
import afsm.sample.shop.core.model.UserSession

class AuthRepository(
    private val userDao: UserDao,
) {
    suspend fun register(
        name: String,
        email: String,
        password: String,
    ): Result<UserSession> {
        val normalizedEmail = email.trim().lowercase()
        if (userDao.findByEmail(normalizedEmail) != null) {
            return Result.failure(IllegalArgumentException("Email is already registered."))
        }

        val userId = userDao.insert(
            UserEntity(
                name = name.trim(),
                email = normalizedEmail,
                passwordHash = hashPassword(password),
                createdAtMillis = System.currentTimeMillis(),
            ),
        )

        return Result.success(
            UserSession(
                userId = userId,
                name = name.trim(),
                email = normalizedEmail,
            ),
        )
    }

    suspend fun login(
        email: String,
        password: String,
    ): Result<UserSession> {
        val normalizedEmail = email.trim().lowercase()
        val user = userDao.findByEmail(normalizedEmail)
            ?: return Result.failure(IllegalArgumentException("No account matches this email."))

        if (user.passwordHash != hashPassword(password)) {
            return Result.failure(IllegalArgumentException("Password does not match."))
        }

        return Result.success(user.toSession())
    }

    private fun UserEntity.toSession(): UserSession {
        return UserSession(
            userId = id,
            name = name,
            email = email,
        )
    }

    private fun hashPassword(rawPassword: String): String {
        return rawPassword.reversed() + ":sample-only"
    }
}
