package com.shinian.app.core.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.SystemClock
import com.shinian.app.data.model.Note
import com.shinian.app.data.model.NoteType
import com.shinian.app.data.repository.NoteRepository
import com.shinian.app.domain.asr.ASRService
import com.shinian.app.domain.llm.LLMService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val asrService: ASRService,
    private val llmService: LLMService
) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recordingStartTime: Long = 0
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timerJob: Job? = null

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    fun startMeetingMode() {
        if (_recordingState.value !is RecordingState.Idle) return
        
        startRecording(RecordingMode.MEETING)
    }

    fun startFlashMode() {
        if (_recordingState.value !is RecordingState.Idle) return
        
        startRecording(RecordingMode.FLASH)
    }

    fun stopRecording() {
        val currentState = _recordingState.value
        if (currentState !is RecordingState.Recording) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            timerJob?.cancel()

            _recordingState.value = RecordingState.Processing

            outputFile?.let { file ->
                processRecording(file, currentState.mode)
            }
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(e.message)
        }
    }

    private fun startRecording(mode: RecordingMode) {
        try {
            outputFile = createOutputFile()
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }

            recordingStartTime = SystemClock.elapsedRealtime()
            _recordingState.value = when (mode) {
                RecordingMode.MEETING -> RecordingState.Meeting(0)
                RecordingMode.FLASH -> RecordingState.Flash(0)
            }

            timerJob = recordingScope.launch {
                while (isActive) {
                    delay(1000)
                    val elapsed = (SystemClock.elapsedRealtime() - recordingStartTime) / 1000
                    val currentState = _recordingState.value
                    _recordingState.value = when (currentState) {
                        is RecordingState.Meeting -> RecordingState.Meeting(elapsed)
                        is RecordingState.Flash -> RecordingState.Flash(elapsed)
                        else -> currentState
                    }
                }
            }
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(e.message)
        }
    }

    private fun createOutputFile(): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "recording_${System.currentTimeMillis()}.m4a")
    }

    private fun processRecording(audioFile: File, mode: RecordingMode) {
        recordingScope.launch {
            try {
                // 1. ASR 转写
                val transcript = asrService.transcribe(audioFile)
                
                // 2. 意图分类
                val duration = audioFile.length() // 文件大小作为时长参考
                val noteType = classifyIntent(transcript, duration, mode)
                
                // 3. LLM 结构化
                val structured = llmService.structure(transcript, noteType)
                
                // 4. 保存笔记
                val note = Note(
                    id = UUID.randomUUID().toString(),
                    type = noteType,
                    title = structured.title,
                    content = structured.content,
                    audioPath = audioFile.absolutePath,
                    createdAt = System.currentTimeMillis(),
                    isSynced = false
                )
                noteRepository.save(note)
                
                _recordingState.value = RecordingState.Completed(note.id)
            } catch (e: Exception) {
                _recordingState.value = RecordingState.Error(e.message)
            }
        }
    }

    private fun classifyIntent(transcript: String, duration: Long, mode: RecordingMode): NoteType {
        return when {
            mode == RecordingMode.MEETING || duration > 60_000 -> NoteType.MEETING
            transcript.contains("买") || transcript.contains("记得") || 
            transcript.contains("别忘了") || transcript.contains("待办") -> NoteType.TODO
            else -> NoteType.IDEA
        }
    }
}

sealed class RecordingState {
    object Idle : RecordingState()
    data class Meeting(val recordingTime: Long) : RecordingState()
    data class Flash(val recordingTime: Long) : RecordingState()
    object Processing : RecordingState()
    data class Completed(val noteId: String) : RecordingState()
    data class Error(val message: String?) : RecordingState()
}

enum class RecordingMode {
    MEETING,
    FLASH
}