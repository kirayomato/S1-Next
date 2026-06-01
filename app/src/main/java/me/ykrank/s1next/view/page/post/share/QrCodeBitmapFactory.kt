package me.ykrank.s1next.view.page.post.share

import android.graphics.Bitmap
import android.graphics.Color
import java.nio.charset.StandardCharsets
import kotlin.math.max

/**
 * Minimal QR encoder for share-card links. It intentionally supports only
 * version 5 / ECC-L / byte mode, which comfortably fits the local thread URL.
 */
class QrCodeBitmapFactory {

    fun create(content: String, moduleSize: Int): Bitmap {
        val data = encodeData(content)
        val ecc = reedSolomon(data, ECC_CODEWORDS)
        val codewords = data + ecc
        val modules = Array(SIZE) { BooleanArray(SIZE) }
        val reserved = Array(SIZE) { BooleanArray(SIZE) }

        drawFunctionPatterns(modules, reserved)
        drawCodewords(modules, reserved, codewords)
        drawFormatBits(modules, reserved)
        return toBitmap(modules, max(moduleSize, 1))
    }

    private fun encodeData(content: String): IntArray {
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_BYTE_LENGTH) { "QR content is too long" }
        val bits = mutableListOf<Int>()
        appendBits(bits, 0b0100, 4)
        appendBits(bits, bytes.size, 8)
        bytes.forEach {
            appendBits(bits, it.toInt() and 0xFF, 8)
        }
        repeat(minOf(4, DATA_CODEWORDS * 8 - bits.size)) {
            bits += 0
        }
        while (bits.size % 8 != 0) {
            bits += 0
        }
        val data = mutableListOf<Int>()
        bits.chunked(8).forEach { byteBits ->
            data += byteBits.fold(0) { acc, bit -> (acc shl 1) or bit }
        }
        var pad = 0
        while (data.size < DATA_CODEWORDS) {
            data += if (pad++ % 2 == 0) 0xEC else 0x11
        }
        return data.toIntArray()
    }

    private fun appendBits(bits: MutableList<Int>, value: Int, count: Int) {
        for (i in count - 1 downTo 0) {
            bits += (value ushr i) and 1
        }
    }

    private fun drawFunctionPatterns(modules: Array<BooleanArray>, reserved: Array<BooleanArray>) {
        drawFinder(modules, reserved, 0, 0)
        drawFinder(modules, reserved, SIZE - 7, 0)
        drawFinder(modules, reserved, 0, SIZE - 7)
        drawAlignment(modules, reserved, 30, 30)

        for (i in 0 until SIZE) {
            setReserved(reserved, 6, i)
            setReserved(reserved, i, 6)
            if (i !in 0..7 && i !in SIZE - 8 until SIZE) {
                modules[6][i] = i % 2 == 0
            }
            if (i !in 0..7 && i !in SIZE - 8 until SIZE) {
                modules[i][6] = i % 2 == 0
            }
        }
        setFunction(modules, reserved, 8, VERSION * 4 + 9, true)
        reserveFormatAreas(reserved)
    }

    private fun drawFinder(
        modules: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        left: Int,
        top: Int
    ) {
        for (dy in -1..7) {
            for (dx in -1..7) {
                val x = left + dx
                val y = top + dy
                if (x !in 0 until SIZE || y !in 0 until SIZE) {
                    continue
                }
                val black = dx in 0..6 && dy in 0..6 &&
                    (dx == 0 || dx == 6 || dy == 0 || dy == 6 || (dx in 2..4 && dy in 2..4))
                setFunction(modules, reserved, x, y, black)
            }
        }
    }

    private fun drawAlignment(
        modules: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        centerX: Int,
        centerY: Int
    ) {
        for (dy in -2..2) {
            for (dx in -2..2) {
                val black = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1
                setFunction(modules, reserved, centerX + dx, centerY + dy, black)
            }
        }
    }

    private fun reserveFormatAreas(reserved: Array<BooleanArray>) {
        for (i in 0..8) {
            if (i != 6) {
                setReserved(reserved, 8, i)
                setReserved(reserved, i, 8)
            }
        }
        for (i in SIZE - 8 until SIZE) {
            setReserved(reserved, 8, i)
            setReserved(reserved, i, 8)
        }
    }

    private fun drawCodewords(
        modules: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        codewords: IntArray
    ) {
        var bitIndex = 0
        var upward = true
        var x = SIZE - 1
        while (x > 0) {
            if (x == 6) {
                x--
            }
            for (i in 0 until SIZE) {
                val y = if (upward) SIZE - 1 - i else i
                for (dx in 0..1) {
                    val xx = x - dx
                    if (reserved[y][xx]) {
                        continue
                    }
                    val bit = if (bitIndex < codewords.size * 8) {
                        ((codewords[bitIndex / 8] ushr (7 - bitIndex % 8)) and 1) == 1
                    } else {
                        false
                    }
                    modules[y][xx] = bit xor mask(xx, y)
                    bitIndex++
                }
            }
            upward = !upward
            x -= 2
        }
    }

    private fun drawFormatBits(modules: Array<BooleanArray>, reserved: Array<BooleanArray>) {
        val bits = formatBits()
        fun bit(i: Int) = ((bits ushr i) and 1) != 0
        for (i in 0..5) setFunction(modules, reserved, 8, i, bit(i))
        setFunction(modules, reserved, 8, 7, bit(6))
        setFunction(modules, reserved, 8, 8, bit(7))
        setFunction(modules, reserved, 7, 8, bit(8))
        for (i in 9 until 15) setFunction(modules, reserved, 14 - i, 8, bit(i))

        for (i in 0 until 8) setFunction(modules, reserved, SIZE - 1 - i, 8, bit(i))
        for (i in 8 until 15) setFunction(modules, reserved, 8, SIZE - 15 + i, bit(i))
        setFunction(modules, reserved, 8, SIZE - 8, true)
    }

    private fun formatBits(): Int {
        var data = (ECC_LEVEL_L_BITS shl 3) or MASK_PATTERN
        var bits = data shl 10
        for (i in 14 downTo 10) {
            if (((bits ushr i) and 1) != 0) {
                bits = bits xor (FORMAT_POLY shl (i - 10))
            }
        }
        return ((data shl 10) or bits) xor FORMAT_MASK
    }

    private fun reedSolomon(data: IntArray, degree: Int): IntArray {
        val generator = reedSolomonGenerator(degree)
        val result = IntArray(degree)
        data.forEach { value ->
            val factor = value xor result[0]
            result.copyInto(result, 0, 1)
            result[degree - 1] = 0
            for (i in 0 until degree) {
                result[i] = result[i] xor multiply(generator[i], factor)
            }
        }
        return result
    }

    private fun reedSolomonGenerator(degree: Int): IntArray {
        var result = intArrayOf(1)
        var root = 1
        repeat(degree) {
            val next = IntArray(result.size + 1)
            for (i in result.indices) {
                next[i] = next[i] xor multiply(result[i], root)
                next[i + 1] = next[i + 1] xor result[i]
            }
            result = next
            root = multiply(root, 0x02)
        }
        return result
    }

    private fun multiply(x: Int, y: Int): Int {
        var a = x
        var b = y
        var product = 0
        while (b != 0) {
            if ((b and 1) != 0) {
                product = product xor a
            }
            a = a shl 1
            if ((a and 0x100) != 0) {
                a = a xor 0x11D
            }
            b = b ushr 1
        }
        return product and 0xFF
    }

    private fun toBitmap(modules: Array<BooleanArray>, moduleSize: Int): Bitmap {
        val bitmapSize = (SIZE + QUIET_ZONE * 2) * moduleSize
        val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                if (!modules[y][x]) {
                    continue
                }
                for (dy in 0 until moduleSize) {
                    for (dx in 0 until moduleSize) {
                        bitmap.setPixel(
                            (x + QUIET_ZONE) * moduleSize + dx,
                            (y + QUIET_ZONE) * moduleSize + dy,
                            Color.BLACK
                        )
                    }
                }
            }
        }
        return bitmap
    }

    private fun setFunction(
        modules: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        x: Int,
        y: Int,
        black: Boolean
    ) {
        modules[y][x] = black
        setReserved(reserved, x, y)
    }

    private fun setReserved(reserved: Array<BooleanArray>, x: Int, y: Int) {
        if (x in 0 until SIZE && y in 0 until SIZE) {
            reserved[y][x] = true
        }
    }

    private fun mask(x: Int, y: Int): Boolean = (x + y) % 2 == 0

    companion object {
        private const val VERSION = 5
        private const val SIZE = VERSION * 4 + 17
        private const val QUIET_ZONE = 4
        private const val DATA_CODEWORDS = 108
        private const val ECC_CODEWORDS = 26
        private const val MAX_BYTE_LENGTH = 106
        private const val ECC_LEVEL_L_BITS = 0b01
        private const val MASK_PATTERN = 0
        private const val FORMAT_POLY = 0x537
        private const val FORMAT_MASK = 0x5412
    }
}
