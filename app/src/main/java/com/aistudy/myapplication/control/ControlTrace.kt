package com.aistudy.myapplication.control

internal fun ByteArray.controlHex():String = buildString(size*2) {
    val digits="0123456789abcdef"
    for(byte in this@controlHex){val value=byte.toInt() and 255;append(digits[value ushr 4]);append(digits[value and 15])}
}
