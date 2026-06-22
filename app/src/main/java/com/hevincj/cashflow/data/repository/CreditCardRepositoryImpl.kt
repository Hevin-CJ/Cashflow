package com.hevincj.cashflow.data.repository

import com.hevincj.cashflow.data.local.dao.CreditCardDao
import com.hevincj.cashflow.data.local.entity.CreditCardEntity
import com.hevincj.cashflow.data.mapper.toDomain
import com.hevincj.cashflow.data.mapper.toEntity
import com.hevincj.cashflow.data.remote.api.CardsApi
import com.hevincj.cashflow.data.remote.models.CreditCardRequestDto
import com.hevincj.cashflow.domain.models.CreditCard
import com.hevincj.cashflow.domain.repository.CreditCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CreditCardRepositoryImpl @Inject constructor(
    private val dao: CreditCardDao,
    private val api: CardsApi
) : CreditCardRepository {

    override fun getCards(): Flow<List<CreditCard>> {
        return dao.getCards().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addCard(card: CreditCard) {
        // Save locally first
        val entity = card.toEntity().copy(isSynced = false)
        val localId = dao.insertCard(entity)
        
        // Try to sync to backend immediately
        try {
            val requestDto = CreditCardRequestDto(
                balance = card.balance,
                cardNumber = card.cardNumber,
                cardHolder = card.cardHolder,
                gradientColors = card.gradientColors
            )
            val response = api.createCard(requestDto)
            if (response.isSuccessful) {
                response.body()?.let { remoteCard ->
                    // Retrieve what we just inserted to preserve local auto-generated primary key
                    val existing = dao.getCards().first().find { 
                        it.cardNumber == card.cardNumber && it.cardHolder == card.cardHolder 
                    }
                    if (existing != null) {
                        dao.insertCard(
                            existing.copy(
                                serverId = remoteCard.id,
                                isSynced = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun syncCards(): String? {
        try {
            // 1. Sync unsynced cards first
            val unsynced = dao.getCards().first().filter { !it.isSynced }
            unsynced.forEach { entity ->
                val requestDto = CreditCardRequestDto(
                    balance = entity.balance,
                    cardNumber = entity.cardNumber,
                    cardHolder = entity.cardHolder,
                    gradientColors = entity.gradientColorsHex.split(",").map { it.toLong(16) }
                )
                val response = api.createCard(requestDto)
                if (response.isSuccessful) {
                    response.body()?.let { remoteCard ->
                        dao.insertCard(
                            entity.copy(
                                serverId = remoteCard.id,
                                isSynced = true
                            )
                        )
                    }
                }
            }

            // 2. Fetch all cards from server
            val response = api.getCards()
            if (response.isSuccessful) {
                response.body()?.let { remoteCards ->
                    // Map remote list to set of serverIds
                    val remoteServerIds = remoteCards.map { it.id }.toSet()

                    // Delete local cards that were deleted on the server (orphans)
                    val localCards = dao.getCards().first()
                    localCards.forEach { local ->
                        if (local.isSynced && (local.serverId == null || local.serverId !in remoteServerIds)) {
                            dao.deleteCard(local.id)
                        }
                    }
                    
                    // Insert or update remote cards into Room
                    remoteCards.forEach { remote ->
                        val existing = localCards.find { it.serverId == remote.id }
                        val entityToInsert = CreditCardEntity(
                            id = existing?.id ?: 0,
                            balance = remote.balance,
                            cardNumber = remote.cardNumber,
                            cardHolder = remote.cardHolder,
                            gradientColorsHex = remote.gradientColors.joinToString(",") { it.toString(16) },
                            serverId = remote.id,
                            isSynced = true
                        )
                        dao.insertCard(entityToInsert)
                    }
                }
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return when (e) {
                is java.net.UnknownHostException -> "No internet connection"
                else -> "Server is unreachable"
            }
        }
    }
}
