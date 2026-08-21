package com.slimenull.androidsystemsoundstudio.data

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

object OggAudioTranscoder {
    private const val TIMEOUT_US = 10_000L
    private const val TARGET_SAMPLE_RATE = 48_000
    private const val MAX_DURATION_US = 60_000_000L
    private const val MAX_PCM_BYTES = 32 * 1024 * 1024

    val isAvailable: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { codec ->
                codec.isEncoder && codec.supportedTypes.any {
                    it.equals(MediaFormat.MIMETYPE_AUDIO_OPUS, ignoreCase = true)
                }
            }
    }

    fun transcode(input: File, output: File) {
        check(isAvailable) { "当前设备不支持将音频转换为 OGG" }
        val decoded = decodeToPcm(input)
        val pcm = if (decoded.sampleRate == TARGET_SAMPLE_RATE) {
            decoded.data
        } else {
            resamplePcm16(
                input = decoded.data,
                channelCount = decoded.channelCount,
                sourceRate = decoded.sampleRate,
                targetRate = TARGET_SAMPLE_RATE,
            )
        }
        try {
            encodeOpusOgg(pcm, decoded.channelCount, output)
        } catch (error: Throwable) {
            output.delete()
            throw error
        }
    }

    private fun decodeToPcm(input: File): PcmAudio {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(input.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("所选文件不包含可识别的音轨")
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: error("无法识别音频格式")
            val durationUs = inputFormat.longValueOrNull(MediaFormat.KEY_DURATION)
            require(durationUs == null || durationUs <= MAX_DURATION_US) { "仅支持最长 60 秒的系统音效" }

            var sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            require(channelCount in 1..2) { "仅支持单声道或双声道音频" }

            extractor.selectTrack(trackIndex)
            decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(inputFormat, null, null, 0)
                start()
            }

            val output = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false

            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = requireNotNull(decoder.getInputBuffer(inputIndex))
                        val size = extractor.readSampleData(inputBuffer, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputEnded = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = decoder.outputFormat
                        sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        val pcmEncoding = outputFormat.integerValueOrNull(MediaFormat.KEY_PCM_ENCODING)
                            ?: AudioFormat.ENCODING_PCM_16BIT
                        require(pcmEncoding == AudioFormat.ENCODING_PCM_16BIT) {
                            "设备解码器未提供 16 位 PCM 输出"
                        }
                        require(channelCount in 1..2) { "仅支持单声道或双声道音频" }
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        if (bufferInfo.size > 0 && bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            val outputBuffer = requireNotNull(decoder.getOutputBuffer(outputIndex)).apply {
                                position(bufferInfo.offset)
                                limit(bufferInfo.offset + bufferInfo.size)
                            }
                            val bytes = ByteArray(bufferInfo.size)
                            outputBuffer.get(bytes)
                            output.write(bytes)
                            require(output.size() <= MAX_PCM_BYTES) { "音频解码后数据过大" }
                        }
                        outputEnded = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }

            val data = output.toByteArray()
            require(data.isNotEmpty()) { "音频中没有可转换的数据" }
            return PcmAudio(data, sampleRate, channelCount)
        } finally {
            runCatching { decoder?.stop() }
            decoder?.release()
            extractor.release()
        }
    }

    private fun encodeOpusOgg(pcm: ByteArray, channelCount: Int, output: File) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val bytesPerFrame = channelCount * 2
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_OPUS,
            TARGET_SAMPLE_RATE,
            channelCount,
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, if (channelCount == 1) 64_000 else 96_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)
        var encoderStarted = false
        var muxerStarted = false
        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            encoderStarted = true

            val bufferInfo = MediaCodec.BufferInfo()
            var inputOffset = 0
            var inputEnded = false
            var outputEnded = false
            var muxerTrack = -1

            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = requireNotNull(encoder.getInputBuffer(inputIndex)).apply { clear() }
                        if (inputOffset >= pcm.size) {
                            val presentationTimeUs = framesToUs(inputOffset / bytesPerFrame)
                            encoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                presentationTimeUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputEnded = true
                        } else {
                            var size = min(inputBuffer.remaining(), pcm.size - inputOffset)
                            size -= size % bytesPerFrame
                            check(size > 0) { "编码器输入缓冲区过小" }
                            inputBuffer.put(pcm, inputOffset, size)
                            val presentationTimeUs = framesToUs(inputOffset / bytesPerFrame)
                            encoder.queueInputBuffer(inputIndex, 0, size, presentationTimeUs, 0)
                            inputOffset += size
                        }
                    }
                }

                when (val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        check(!muxerStarted) { "编码器重复返回输出格式" }
                        muxerTrack = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        val outputBuffer = requireNotNull(encoder.getOutputBuffer(outputIndex))
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0) {
                            check(muxerStarted) { "编码器尚未提供 OGG 输出格式" }
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(muxerTrack, outputBuffer, bufferInfo)
                        }
                        outputEnded = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        encoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
        } finally {
            if (encoderStarted) runCatching { encoder.stop() }
            encoder.release()
            if (muxerStarted) runCatching { muxer.stop() }
            muxer.release()
        }
    }

    private fun resamplePcm16(
        input: ByteArray,
        channelCount: Int,
        sourceRate: Int,
        targetRate: Int,
    ): ByteArray {
        val bytesPerFrame = channelCount * 2
        val inputFrames = input.size / bytesPerFrame
        val outputFrames = (inputFrames.toDouble() * targetRate / sourceRate).roundToInt()
        val output = ByteArray(outputFrames * bytesPerFrame)

        repeat(outputFrames) { outputFrame ->
            val sourcePosition = outputFrame.toDouble() * sourceRate / targetRate
            val firstFrame = floor(sourcePosition).toInt().coerceIn(0, inputFrames - 1)
            val secondFrame = min(firstFrame + 1, inputFrames - 1)
            val fraction = sourcePosition - firstFrame

            repeat(channelCount) { channel ->
                val first = readPcm16(input, (firstFrame * channelCount + channel) * 2)
                val second = readPcm16(input, (secondFrame * channelCount + channel) * 2)
                val sample = (first + (second - first) * fraction).roundToInt().coerceIn(-32_768, 32_767)
                val offset = (outputFrame * channelCount + channel) * 2
                output[offset] = sample.toByte()
                output[offset + 1] = (sample shr 8).toByte()
            }
        }
        return output
    }

    private fun readPcm16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or (bytes[offset + 1].toInt() shl 8)

    private fun framesToUs(frameCount: Int): Long =
        frameCount.toLong() * 1_000_000L / TARGET_SAMPLE_RATE

    private fun MediaFormat.integerValueOrNull(key: String): Int? =
        if (containsKey(key)) getInteger(key) else null

    private fun MediaFormat.longValueOrNull(key: String): Long? =
        if (containsKey(key)) getLong(key) else null

    private data class PcmAudio(
        val data: ByteArray,
        val sampleRate: Int,
        val channelCount: Int,
    )
}
