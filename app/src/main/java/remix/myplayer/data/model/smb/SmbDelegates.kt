package remix.myplayer.data.model.smb

import java.io.Closeable

interface SmbStreamDelegate : Closeable {
    fun open(url: String, offset: Long): Long
    fun read(buffer: ByteArray, offset: Int, length: Int): Int
}

interface SmbRandomAccessDelegate : Closeable {
    fun open(url: String)
    fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int
    fun getSize(): Long
}
