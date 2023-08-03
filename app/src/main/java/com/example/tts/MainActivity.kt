package com.example.tts

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tts.databinding.ActivityMainBinding
import com.example.tts.ui.adapter.TestItemAdapter
import com.example.tts.ui.viewmodel.MainViewModel
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var listAdapter: TestItemAdapter

    private var textToSpeech: TextToSpeech? = null
    private var languageIdentifier: LanguageIdentifier? = null

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        languageIdentifier = LanguageIdentification.getClient()

        textToSpeech = TextToSpeech(
            this
        ) {
            if (it == TextToSpeech.SUCCESS) {
                val availableLanguages =
                    textToSpeech?.availableLanguages?.map { locale -> locale.displayLanguage }
                        .toString()
                binding.tvSupportLanguages.text = availableLanguages
            } else {
                binding.tvSupportLanguages.text = "语音合成引擎初始化失败, 无法识别可用的语音包"
            }
        }

        listAdapter = TestItemAdapter().apply {
            onItemClicked = { data ->
                textToSpeech?.let {
                    if (it.isSpeaking) {
                        it.stop()
                    }
                    // 识别文本的语言
                    languageIdentifier?.let { identifier ->
                        identifier.identifyLanguage(data.content)
                            .addOnSuccessListener { languageCode ->
                                // 成功获取识别的语言代码 languageCode
                                // 可以根据 languageCode 设置 TTS 的语言
                                val result = it.setLanguage(Locale.forLanguageTag(languageCode))
                                if (result == TextToSpeech.LANG_MISSING_DATA
                                    || result == TextToSpeech.LANG_NOT_SUPPORTED
                                ) {
                                    // 处理识别出来的 languageCode 是 und 或者
                                    // languageCode 不是文本对应的语言且系统没有对应的语言包的情况
                                    // languageCode 是文本对应的语言且系统没有对应的语言包的情况也会调用该处理，
                                    // 该情况无法避免，因为此处无法知道识别的 languageCode 是否正确。
                                    identifyPossibleLanguages(data.content)
                                } else {
                                    val code = textToSpeech?.speak(
                                        data.content,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        null
                                    )
                                    if (code == TextToSpeech.ERROR) {
                                        // 处理识别出来的 languageCode 不是文本对应的语言的情况
                                        identifyPossibleLanguages(data.content)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                // 处理语言识别失败的情况
                                identifyPossibleLanguages(data.content)
                            }
                    }
                }

            }
        }

        binding.recyclerView.apply {
            layoutManager =
                LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            adapter = listAdapter
        }

        viewModel.getDataListLiveData().observe(this) {
            listAdapter.submitList(it)
        }

        viewModel.getData()

        binding.tvSpeed1.setOnClickListener {
            it.setBackgroundResource(R.drawable.shape_bg_item_list)
            binding.tvSpeed2.setBackgroundDrawable(null)
            binding.tvSpeed3.setBackgroundDrawable(null)
            binding.tvSpeed4.setBackgroundDrawable(null)

            textToSpeech?.setSpeechRate(binding.tvSpeed1.text.toString().toFloat())
        }

        binding.tvSpeed2.setOnClickListener {
            it.setBackgroundResource(R.drawable.shape_bg_item_list)
            binding.tvSpeed1.setBackgroundDrawable(null)
            binding.tvSpeed3.setBackgroundDrawable(null)
            binding.tvSpeed4.setBackgroundDrawable(null)

            textToSpeech?.setSpeechRate(binding.tvSpeed2.text.toString().toFloat())
        }

        binding.tvSpeed3.setOnClickListener {
            it.setBackgroundResource(R.drawable.shape_bg_item_list)
            binding.tvSpeed1.setBackgroundDrawable(null)
            binding.tvSpeed2.setBackgroundDrawable(null)
            binding.tvSpeed4.setBackgroundDrawable(null)

            textToSpeech?.setSpeechRate(binding.tvSpeed3.text.toString().toFloat())
        }

        binding.tvSpeed4.setOnClickListener {
            it.setBackgroundResource(R.drawable.shape_bg_item_list)
            binding.tvSpeed1.setBackgroundDrawable(null)
            binding.tvSpeed2.setBackgroundDrawable(null)
            binding.tvSpeed3.setBackgroundDrawable(null)

            textToSpeech?.setSpeechRate(binding.tvSpeed4.text.toString().toFloat())
        }
    }

    private fun identifyPossibleLanguages(content: String) {
        languageIdentifier?.let { identifier ->
            identifier.identifyPossibleLanguages(content)
                .addOnSuccessListener { identifierLanguageList ->
                    for (i in 0 until identifierLanguageList.size) {
                        val languageTag =
                            identifierLanguageList[i].languageTag
                        val result =
                            textToSpeech?.setLanguage(
                                Locale.forLanguageTag(
                                    languageTag
                                )
                            ) ?: TextToSpeech.LANG_NOT_SUPPORTED
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            if (i == identifierLanguageList.size - 1) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "语言不支持，$languageTag",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            continue
                        }
                        val code = textToSpeech?.speak(
                            content,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        ) ?: TextToSpeech.ERROR
                        if (code == TextToSpeech.SUCCESS) {
                            break
                        }
                    }
                }
                .addOnFailureListener {
                    // 识别语言失败
                    Toast.makeText(
                        this@MainActivity,
                        "语言识别失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    override fun onDestroy() {
        textToSpeech?.let {
            it.stop()
            it.shutdown()
        }
        super.onDestroy()
    }
}