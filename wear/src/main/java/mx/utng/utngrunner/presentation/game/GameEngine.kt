package mx.utng.utngrunner.presentation.game

import mx.utng.utngrunner.domain.model.Coin
import mx.utng.utngrunner.domain.model.GamePhase
import mx.utng.utngrunner.domain.model.GameState
import mx.utng.utngrunner.domain.model.Obstacle
import mx.utng.utngrunner.domain.model.ObstacleType
import mx.utng.utngrunner.domain.model.Player

/**
 * GameEngine: FUNCIÓN PURA.
 * - No conoce Canvas, Composables ni Android.
 * - Recibe un GameState y retorna el siguiente GameState.
 * - Fácil de testear unitariamente.
 */
object GameEngine {

    fun update(state: GameState, frame: Long): GameState {
        if (state.phase != GamePhase.PLAYING) return state

        val updatedPlayer  = updatePlayer(state.player)
        val newScore       = state.score + 1
        val newLevel       = calculateLevel(newScore)
        val newSpeed       = 3f + newLevel * 0.6f
        val updatedObs     = updateObstacles(state.obstacles, newSpeed, frame)
        val updatedCoins   = updateCoins(state.coins, newSpeed)
        val afterCollision = checkCollisions(
            updatedPlayer, updatedObs, updatedCoins, state.lives
        )

        return state.copy(
            player    = afterCollision.player,
            score     = newScore,
            level     = newLevel,
            lives     = afterCollision.lives,
            gameSpeed = newSpeed,
            obstacles = afterCollision.obstacles,
            coins     = afterCollision.coins,
            phase     = if (afterCollision.lives <= 0) GamePhase.DEAD
                        else GamePhase.PLAYING
        )
    }

    private fun updatePlayer(p: Player): Player {
        val newVelY = p.velocityY + Player.GRAVITY
        val newY    = (p.y + newVelY).coerceAtMost(Player.FLOOR_Y)
        val landed  = newY >= Player.FLOOR_Y
        return p.copy(
            y           = newY,
            velocityY   = if (landed) 0f else newVelY,
            isJumping   = !landed && p.isJumping,
            isSliding   = p.slideFrames > 0,
            slideFrames = (p.slideFrames - 1).coerceAtLeast(0),
            isInvincible    = p.invincibleFrames > 0,
            invincibleFrames= (p.invincibleFrames - 1).coerceAtLeast(0)
        )
    }

    private fun calculateLevel(score: Int): Int =
        (1 + score / 300).coerceAtMost(5)

    private fun updateObstacles(
        obstacles: List<Obstacle>, speed: Float, frame: Long
    ): List<Obstacle> {
        val moved = obstacles
            .map { it.copy(x = it.x - speed) }
            .filter { it.x > -50f }
        // Spawn probabilístico
        return if (frame % 60 == 0L && Math.random() < 0.6) {
            val type = ObstacleType.entries.random()
            moved + Obstacle(x = 240f, width = type.w, height = type.h, type = type)
        } else moved
    }
    
    private fun updateCoins(coins: List<Coin>, speed: Float): List<Coin> {
        return coins.map { it.copy(x = it.x - speed) }.filter { it.x > -50f }
    }

    /** Detección AABB (Axis-Aligned Bounding Box) */
    private fun checkCollisions(
        player: Player, obstacles: List<Obstacle>, coins: List<Coin>, currentLives: Int
    ): CollisionResult {
        var lives = currentLives
        val FLOOR = Player.FLOOR_Y + 20f
        val cLeft  = player.x - 10f
        val cRight = player.x + 18f
        val cTop   = player.y - (if (player.isSliding) 8f else 30f)
        val cBot   = player.y + 20f

        val hitObs = obstacles.filter { o ->
            !player.isInvincible &&
            cRight > o.x + 4f && cLeft < o.x + o.width - 4f &&
            cBot > FLOOR - o.height && cTop < FLOOR
        }

        val updatedCoins = coins.map { cn ->
            if (!cn.collected && Math.hypot((player.x - cn.x).toDouble(),
                (player.y - cn.y).toDouble()) < 22.0)
                cn.copy(collected = true) else cn
        }

        return CollisionResult(
            player    = if (hitObs.isNotEmpty()) player.copy(invincibleFrames = Player.INVINCIBLE_FRAMES) else player,
            lives     = if (hitObs.isNotEmpty()) lives - hitObs.size else lives,
            obstacles = obstacles.map { if (it in hitObs) it.copy(x = -999f) else it },
            coins     = updatedCoins
        )
    }
}

data class CollisionResult(val player: Player, val lives: Int,
                           val obstacles: List<Obstacle>, val coins: List<Coin>)
