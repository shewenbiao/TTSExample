package com.example.tts.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tts.model.bean.TestItem
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val dataList = listOf(
        TestItem("中文", "zh", "你好，中国。我爱中国。"),
        TestItem("英文", "en", "Hello, China. I love China."),
        TestItem("葡语", "pt", "Olá, China. Adoro a China."),
        TestItem("法语", "fr", "Bonjour, la Chine. J\'aime la Chine."),
        TestItem("西语", "es", "Hola, china. Me encanta china."),
        TestItem("日语", "ja", "こんにちは、中国。私は中国が好きです。"),
        TestItem("俄语", "ru", "Здравствуйте, Китай.  Я люблю Китай."),
        TestItem("德语", "de", "Hallo, China. Ich liebe China.")
    )

    private val dataListLiveData = MutableLiveData<List<TestItem>>()

    fun getData() {
        viewModelScope.launch {
            dataListLiveData.value = dataList
        }
    }

    fun getDataListLiveData(): MutableLiveData<List<TestItem>> = dataListLiveData
}