Import Kotlin
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array

const val SCREEN_WIDTH = 1080
const val SCREEN_HEIGHT = 720
const val SQUARE_SIZE = 90
const val NUM_SQUARES = 18
const val CENTER_AVOID_AREA_X = 550
const val CENTER_AVOID_AREA_Y = 355
const val CENTER_AVOID_AREA_WIDTH = 100
const val CENTER_AVOID_AREA_HEIGHT = 100
const val START_POSITION_X = 50
const val START_POSITION_Y = 50
const val START_TIME = 30

class CatCatchGame : ApplicationAdapter() {
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont
    lateinit var camera: OrthographicCamera

    lateinit var mainTitle: MainTitle
    var numSquares = NUM_SQUARES
    lateinit var paths: Array<String>
    lateinit var squares: Array<Square>
    lateinit var color: Color
    lateinit var centerAvoidArea: Circle
    lateinit var mainRect: Rectangle
    var startTime = System.currentTimeMillis()

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont()
        camera = OrthographicCamera()
        camera.setToOrtho(false, SCREEN_WIDTH.toFloat(), SCREEN_HEIGHT.toFloat())

        mainTitle = MainTitle()
        paths = generateSquarePaths(numSquares)
        squares = createSquares(numSquares, paths)
        color = Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1f)
        centerAvoidArea = Circle(CENTER_AVOID_AREA_X.toFloat(), CENTER_AVOID_AREA_Y.toFloat(), CENTER_AVOID_AREA_WIDTH.toFloat())
        mainRect = Rectangle(START_POSITION_X.toFloat(), START_POSITION_Y.toFloat(), SQUARE_SIZE.toFloat(), SQUARE_SIZE.toFloat())
    }

    override fun render() {
        handleInput()
        update()
        draw()
    }

    private fun handleInput() {
        if (Gdx.input.isKeyPressed(Keys.ESCAPE)) {
            Gdx.app.exit()
        }
    }

    private fun update() {
        updateSquarePositions(squares, centerAvoidArea, startTime, mainRect)
    }

    private fun draw() {
        Gdx.gl.glClearColor(1f, 0.78f, 0.78f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch.projectionMatrix = camera.combined
        batch.begin()

        mainTitle.draw(batch)
        drawSquares(batch, squares, color)

        val frozenSquare = squares.firstOrNull { it.direction == Direction.FREEZE }
        frozenSquare?.let {
            val circleRadius = Math.max(it.rect.width, it.rect.height) * 1.2f
            val circleCenter = it.rect.center(Vector2())
            drawCircle(batch, Color.RED, circleCenter, circleRadius)
        }

        drawCountdownClock(batch, startTime)

        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
    }

    private fun generateSquarePaths(numPaths: Int): Array<String> {
        val basePath = "B:/Python files misc/Bellas_cat_glossary"
        val filenames = arrayOf(
            "cat.burger.png", "jcat.png", "monstacat.png", "purrito.png", "robocat.png",
            "cookie.cat.png", "game.cat.png", "heart.cat.png", "idk.cat.png", "lava.cat.png",
            "poptart.cat.png", "shroom.cat.png", "star.cat.png", "straw.cat.png", "tongue.cat.png",
            "candy.cat.png", "snow.cat.png", "bears.cat.png"
        )
        return Array(filenames.map { filename -> "$basePath/$filename" }.toTypedArray())
    }

    private fun createSquares(numSquares: Int, paths: Array<String>): Array<Square> {
        val squares = Array<Square>()
        for (i in 0 until numSquares) {
            val square = Square(
                Rectangle(
                    MathUtils.random(70f, SCREEN_WIDTH - SQUARE_SIZE - 70f),
                    MathUtils.random(70f, SCREEN_HEIGHT - SQUARE_SIZE - 70f),
                    SQUARE_SIZE.toFloat(),
                    SQUARE_SIZE.toFloat()
                ),
                paths[i],
                Direction.values().random()
            )
            squares.add(square)
        }
        return squares
    }

    private fun updateSquarePositions(
        squares: Array<Square>,
        centerAvoidArea: Circle,
        startTime: Long,
        mainRect: Rectangle
    ) {
        val elapsedTime = System.currentTimeMillis() - startTime
        val remainingTime = Math.max(0, START_TIME - elapsedTime / 1000)

        if (elapsedTime > startTime + 30000) {
            // If more than 30 seconds have passed, stop updating positions
            val closestSquare = findClosestSquare(mainRect, squares, centerAvoidArea)
            closestSquare?.let {
                displayFileName(it.path)
                if (isInCenterAvoidArea(it.rect, centerAvoidArea)) {
                    it.direction = Direction.FREEZE
                }
            }
            return
        }

        for (square in squares) {
            val squareSpeed = MathUtils.random(2f, 50f)

            // Update position based on direction
            when (square.direction) {
                Direction.LEFT -> if (square.rect.x > 0) square.rect.x -= squareSpeed
                Direction.RIGHT -> if (square.rect.x + square.rect.width < SCREEN_WIDTH) square.rect.x += squareSpeed
                Direction.UP -> if (square.rect.y > 0) square.rect.y -= squareSpeed
                Direction.DOWN -> if (square.rect.y + square.rect.height < SCREEN_HEIGHT) square.rect.y += squareSpeed
            }

            // Check for collisions with other squares
            for (otherSquare in squares) {
                if (otherSquare != square && isCollision(square.rect, otherSquare.rect)) {
                    // Adjust direction to avoid collision
                    square.direction = Direction.values().random()
                    break
                }
            }
        }

        // Find the closest square to the "Cat Catch" text
        val closestSquare = findClosestSquare(mainRect, squares, centerAvoidArea)

        // Freeze the closest square not within the center avoid area
        closestSquare?.let {
            if (isInCenterAvoidArea(it.rect, centerAvoidArea)) {
                it.direction = Direction.FREEZE
            }
        }
    }

    private fun drawSquares(batch: SpriteBatch, squares: Array<Square>, color: Color) {
        for (square in squares) {
            batch.color = color
            batch.draw(squareTexture, square.rect.x, square.rect.y, square.rect.width, square.rect.height)

            // Load thumbnail and draw
            val thumbnail = Texture(square.path)
            batch.draw(thumbnail, square.rect.x, square.rect.y, square.rect.width * 0.8f, square.rect.height * 0.8f)

            thumbnail.dispose()
        }
    }

    private fun drawCircle(batch: SpriteBatch, color: Color, center: Vector2, radius: Float) {
        batch.color = color
        val segments = 30
        val circleVertices = FloatArray(segments * 2)
        for (i in 0 until segments * 2 step 2) {
            val angle = MathUtils.PI2 / segments * (i / 2)
            circleVertices[i] = center.x + MathUtils.cos(angle) * radius
            circleVertices[i + 1] = center.y + MathUtils.sin(angle) * radius
        }
        batch.draw(circleTexture, circleVertices, 0, circleVertices.size)
    }

    private fun drawCountdownClock(batch: SpriteBatch, startTime: Long) {
        val elapsedTime = System.currentTimeMillis() - startTime
        font.draw(batch, "Time: $remainingTime s", SCREEN_WIDTH - 130f, SCREEN_HEIGHT - 70f)
    }

    private fun displayFileName(path: String) {
        val fileName = path.substringAfterLast('/')
        val fileNameWithoutExtension = fileName.substringBeforeLast('.')
        font.draw(batch, fileNameWithoutExtension, SCREEN_WIDTH / 2f, 150f)
    }

    private fun findClosestSquare(mainRect: Rectangle, squares: Array<Square>, centerAvoidArea: Circle): Square? {
        var minDistance = Float.MAX_VALUE
        var closestSquare: Square? = null
        val mainCenter = Vector2(mainRect.x + mainRect.width / 2, mainRect.y + mainRect.height / 2)

        for (square in squares) {
            val squareCenter = Vector2(square.rect.x + square.rect.width / 2, square.rect.y + square.rect.height / 2)
            val distance = mainCenter.dst(squareCenter)

            if (distance < minDistance && isInCenterAvoidArea(square.rect, centerAvoidArea)) {
                minDistance = distance
                closestSquare = square
            }
        }

        return closestSquare
    }

    private fun isInCenterAvoidArea(rect: Rectangle, centerArea: Circle): Boolean {
        val circle = Circle(rect.x + rect.width / 2, rect.y + rect.height / 2, Math.max(rect.width, rect.height) / 2)
        return !circle.overlaps(centerArea)
    }

    private fun isCollision(rect1: Rectangle, rect2: Rectangle): Boolean {
        return rect1.overlaps(rect2)
    }
}

class MainTitle {
    private val font: BitmapFont = BitmapFont()

    fun draw(batch: SpriteBatch) {
        font.draw(batch, "Cat Catch", 0f, SCREEN_HEIGHT.toFloat())
    }
}

data class Square(val rect: Rectangle, val path: String, var direction: Direction)

enum class Direction {
    LEFT, RIGHT, UP, DOWN, FREEZE
}
