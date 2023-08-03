package com.example.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView
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

        initSpeedSpinner()
        initPitchSpinner()
    }

    private fun initSpeedSpinner() {
        val adapter = MySpinnerAdapter(this, getSpeedRateData())
        binding.speedSpinner.apply {
            this.adapter = adapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val speedValue = parent.getItemAtPosition(position).toString().toFloat()
                    textToSpeech?.setSpeechRate(speedValue)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // 未选择任何项时的回调
                }
            }
            setSelection(2)
        }
    }

    private fun getSpeedRateData(): List<Float> {
        // 倍速没有明确的上下限
        return listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f)
    }

    private fun initPitchSpinner() {
        val adapter = MySpinnerAdapter(this, getPitchData())
        binding.pitchSpinner.apply {
            this.adapter = adapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val pitchValue = parent.getItemAtPosition(position).toString().toFloat()
                    textToSpeech?.setPitch(pitchValue)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // 未选择任何项时的回调
                }
            }
            setSelection(2)
        }
    }

    private fun getPitchData(): List<Float> {
        // 音调的取值范围是 0.5 到 2.0
        return listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
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

    class MySpinnerAdapter(private val context: Context, private val itemList: List<Float>) : BaseAdapter() {

        override fun getCount(): Int {
            return itemList.size
        }

        override fun getItem(position: Int): Any {
            return itemList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val viewHolder: ViewHolder

            if (convertView == null) {
                view = LayoutInflater.from(context).inflate(R.layout.custom_spinner_item, parent, false)
                viewHolder = ViewHolder(view)
                view.tag = viewHolder
            } else {
                view = convertView
                viewHolder = convertView.tag as ViewHolder
            }

            val item = itemList[position]
            viewHolder.textView.text = item.toString()

            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val viewHolder: ViewHolder

            if (convertView == null) {
                view = LayoutInflater.from(context).inflate(R.layout.custom_spinner_dropdown_item, parent, false)
                viewHolder = ViewHolder(view)
                view.tag = viewHolder
            } else {
                view = convertView
                viewHolder = convertView.tag as ViewHolder
            }

            val item = itemList[position]
            viewHolder.textView.text = item.toString()

            return view
        }

        private class ViewHolder(view: View) {
            val textView: TextView = view.findViewById(R.id.textView)
        }
    }

}