package mx.utng.utngrunner.presentation.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import mx.utng.utngrunner.domain.model.Coin
import mx.utng.utngrunner.domain.model.GameState
import mx.utng.utngrunner.domain.model.Obstacle
import mx.utng.utngrunner.domain.model.Player
import androidx.compose.ui.graphics.nativeCanvas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

/** GameRenderer: SOLO dibuja. No toca la lógica de juego. */
object GameRenderer {

    fun draw(canvas: Canvas, size: Size, state: GameState, frame: Long) {
        drawBackground(canvas, size)
        drawGround(canvas, size)
        drawCoins(canvas, state.coins, frame)
        drawObstacles(canvas, state.obstacles)
        drawPlayer(canvas, state.player, frame)
        drawHUD(canvas, size, state)
    }

    private fun drawBackground(canvas: Canvas, size: Size) {
        // Gradiente de cielo nocturno
        val paint = Paint().apply {
            shader = LinearGradientShader(
                from = Offset(0f, 0f), to = Offset(0f, size.height),
                colors = listOf(Color(0xFF0D1B4A), Color(0xFF1A237E))
            )
        }
        canvas.drawRect(Rect(Offset.Zero, size), paint)
    }

    private fun drawGround(canvas: Canvas, size: Size) {
        val paint = Paint().apply { color = Color(0xFF388E3C) }
        canvas.drawRect(Rect(0f, Player.FLOOR_Y + 14f, size.width, size.height), paint)
    }

    private fun drawCoins(canvas: Canvas, coins: List<Coin>, frame: Long) {
        val paint = Paint().apply { color = Color(0xFFFFD700) }
        coins.forEach { coin ->
            if (!coin.collected) {
                canvas.drawCircle(Offset(coin.x, coin.y), 10f, paint)
            }
        }
    }

    private fun drawObstacles(canvas: Canvas, obstacles: List<Obstacle>) {
        val paint = Paint().apply { color = Color.Red }
        val floor = Player.FLOOR_Y + 14f
        obstacles.forEach { obs ->
            canvas.drawRect(Rect(obs.x, floor - obs.height, obs.x + obs.width, floor), paint)
        }
    }

    private fun drawPlayer(canvas: Canvas, player: Player, frame: Long) {
        // Parpadeo de invencibilidad
        val alpha = if (player.isInvincible && (frame / 4) % 2 == 0L) 0.3f else 1f
        val yPos = player.y

        val bodyPaint = Paint().apply {
            color = Color(0xFFE65100).copy(alpha = alpha)
        }
        // Cuerpo del personaje
        canvas.drawRect(Rect(player.x - 6f, yPos - 10f, player.x + 14f, yPos + 14f), bodyPaint)

        // Casco UTNG
        val helmetPaint = Paint().apply { color = Color(0xFF1A237E).copy(alpha = alpha) }
        canvas.drawRect(Rect(player.x - 5f, yPos - 24f, player.x + 13f, yPos - 14f), helmetPaint)
    }

    private fun drawHUD(canvas: Canvas, size: Size, state: GameState) {
        val cx = size.width / 2f
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 30f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        // Hora del sistema
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        canvas.nativeCanvas.drawText(timeStr, cx, 40f, textPaint)
        
        // Puntuación inferior
        textPaint.textSize = 24f
        canvas.nativeCanvas.drawText("${state.score} pts", cx, size.height - 20f, textPaint)

        // Vidas
        val heartPaint = Paint().apply { color = Color.Red }
        repeat(state.lives) { i ->
            canvas.drawCircle(Offset(40f + i * 20f, 60f), 6f, heartPaint)
        }
        
        // FC
        textPaint.textSize = 20f
        textPaint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.nativeCanvas.drawText("${state.heartRate} bpm", cx + 20f, 80f, textPaint)
    }
}
